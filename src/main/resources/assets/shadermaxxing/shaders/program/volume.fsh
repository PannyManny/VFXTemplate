#version 330 compatibility
#define STEPS 400
#define VOL_COUNT 2
#define VOL_1 0
#define VOL_2 1

// yo test can you see this

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D noiseTexP18;
uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;
uniform float iTime;

in vec2 texCoord;
out vec4 fragColor;

// slop functions
vec3 hsv2rgb( vec3 c )
{
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

mat2 Rotate( float a )
{
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

vec3 worldPos( vec3 point )
{
    vec3 ndc = point * 2.0 - 1.0;
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;
    return (inverse(ModelViewMat) * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

float densityFromSD( float sDistance, float falloff )
{
    return (sDistance < 0.0) ? 1.0 : exp(-sDistance * falloff);
}

float capNoise( vec3 p )
{
    return texture(noiseTexP18, p.xz * 0.05).r * 2.0 - 1.0;
}

// SDF(s)
float sdRoundedCylinder( vec3 p, float ra, float rb, float h )
{
    float noise = capNoise(p);
    float noiseStrength = 1.5;

    float r = length(p.xz);
    float t = smoothstep(0.0, ra, r);
    float heightScale = 1.0 - t;
    float taperedHeight = h * heightScale;
    float noisyHeight = taperedHeight + (noise) * noiseStrength;
    vec2 d = vec2( length(p.xz)-ra+rb, abs(p.y) - (noisyHeight) + rb );

    return min(max(d.x,d.y),0.0) + length(max(d,0.0)) - rb;
}

float sdSphere( vec3 p, float s )
{
    return length(p)-s;
}
//

float sdf[VOL_COUNT];
vec3 localPos[VOL_COUNT];

// Configure movement and position
void computeSDFs( vec3 p )
{
    // Position
    vec3 center1 = p - vec3(0.0, 0.0, 0.0); // x y z coordinates (will be inversed)
    vec3 center2 = p - vec3(0.0, 0.0, 0.0);
    center2.yz *= Rotate(0.1);
    vec3 center3 = p - vec3(0.0, 0.0, 0.0);

    // Movement
    center2.xz *= Rotate(iTime / 17.0);

    // Size/Dimensions
    sdf[VOL_1] = sdSphere(center1, 14.0);
    sdf[VOL_2] = sdRoundedCylinder(center2, 80.0, 1.0, 4.0);

    localPos[VOL_1] = center1;
    localPos[VOL_2] = center2;
}

// Configure appearance
void volumeVisuals( int id, vec3 localPos, out vec3 color, out float baseOpacity, out float falloff )
{
    if (id == VOL_1)
    {
        color = vec3(0.0);
        baseOpacity = 2.0;
        falloff = 99.0;
        return;
    }

    if (id == VOL_2)
    {
        float radius = length(localPos.xz);
        float maxRadius = 100.0;
        float n = texture(noiseTexP18, localPos.xz * 0.1).r;

        // color
        float innerDrop = maxRadius * 0.1;
        float outerDrop = maxRadius * 1.2;

        vec3 hsv = vec3(0.1, 0.2, 0.0);
        float normRad = (radius) / (maxRadius * 1.25);

        hsv.x -= (0.05 * normRad) * 2.5;
        if (hsv.x > 0.16) { hsv.x += (0.05 * normRad) * 2.5; }
        hsv.y += (0.8 * normRad) * 2;
        float tFast = smoothstep(innerDrop, outerDrop, radius);
        float zFast = mix(5, 1, tFast);
        float zSlow = (0.82 * normRad) * 8.5;
        hsv.z = zFast - zSlow;

        vec3 colorA = n * hsv2rgb(hsv);

        // opacity
        float fadeStart = maxRadius / 1.75;
        float fadeEnd = maxRadius / 1.15;
        float edgeFade = 1.0 - smoothstep(fadeStart, fadeEnd, radius);
        float opacityA = n * (edgeFade * 2);

        color = colorA;
        baseOpacity = opacityA;
        falloff = 30.0;
        return;
    }

    // if id isn't found:
    color = vec3(1.0, 0.0, 1.0);
    baseOpacity = 0.0;
    falloff = 1.0;
}

// Configure volume layers
int volumePriority( int id )
{
    // higher number = higher priority
    if (id == VOL_1) return 20;
    if (id == VOL_2) return 10;
    return 0;
}

bool volumeAllowed( int id )
{
    int p = volumePriority(id);
    for (int i = 0; i < VOL_COUNT; i++)
    {
        if (i == id)
        {
            continue;
        }
        if (sdf[i] < 0.0 && volumePriority(i) > p)
        {
            return false;
        }
    }
    return true;
}

vec4 raymarchVolume( vec3 ro, vec3 rd )
{
    float ray = 0.0;
    vec4 accum = vec4(0.0);

    float depthSample = texture(DepthSampler, texCoord).r;
    vec3 hitWorld = worldPos(vec3(texCoord, depthSample)) - BlockPosition;
    float maxDist = length(hitWorld - ro);

    for (int i = 0; i < STEPS; i++)
    {
        vec3 p = ro + rd * ray;
        computeSDFs(p);

        float closestSDF = 1e9;
        for (int v = 0; v < VOL_COUNT; v++)
        {
            closestSDF = min(closestSDF, abs(sdf[v]));
        }

        float rawStep = clamp(closestSDF * 0.6, 0.06, 0.6);
        float smoothen = smoothstep(0.0, 0.4, closestSDF);
        float stepSize = mix(0.06, rawStep, smoothen);

        vec3 toBH = -p;
        float r = length(toBH);
        vec3 dirToBH = toBH / r;

        float rs = 10.0;
        float photonSphere = rs * 1.5;

        float x = photonSphere / max(r, rs);
        float bend = mix(
        pow(x, 1.5),
        pow(x, 4.5),
        smoothstep(0.2, 4.8, x)
        );

        rd += dirToBH * bend * 0.0055 * stepSize;
        rd = normalize(rd);

        for (int id = 0; id < VOL_COUNT; id++)
        {
            if (!volumeAllowed(id))
            {
                continue;
            }

            vec3 color;
            float baseOpacity;
            float falloff;
            volumeVisuals(id, localPos[id], color, baseOpacity, falloff);

            float density = densityFromSD(sdf[id], falloff);
            float alpha = density * baseOpacity * stepSize;

            accum.rgb += (1.0 - accum.a) * color * alpha;
            accum.a += (1.0 - accum.a) * alpha;
        }

        ray += stepSize;
        if (ray > maxDist || accum.a > 0.99) break;
    }

    return accum;
}

void main()
{
    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    float depthSample = texture(DepthSampler, texCoord).r;

    vec3 ro = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3 hitWorld = worldPos(vec3(texCoord, depthSample)) - BlockPosition;
    vec3 rd = normalize(hitWorld - ro);

    vec4 volume = raymarchVolume(ro, rd);

    vec3 finalColor = original * (1.0 - volume.a) + volume.rgb;
    fragColor = vec4(finalColor, 1.0);
}