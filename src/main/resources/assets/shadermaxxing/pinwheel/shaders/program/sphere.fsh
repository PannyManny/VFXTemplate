#version 330
#define STEPS 1600
#define VOL_COUNT 2
#define VOL_1 0
#define VOL_2 1

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D noiseTexP18;
uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;
uniform float iTime;

in vec2 texCoord;
out vec4 fragColor;

// slop functions
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

// SDF(s)
float sdSphere( vec3 p, float r )
{
    return length(p) - r;
}
//

float sdf[VOL_COUNT];
vec3 localPos[VOL_COUNT];

// Configure movement and position
void computeSDFs( vec3 p )
{
    // Position
    vec3 center1 = p - vec3(0.0, 0.0, 0.0); // x y z coordinates relative to command coords (will be inversed)

    // Movement


    // Size/Dimensions
    sdf[VOL_1] = sdSphere(center1, 1.0);

    localPos[VOL_1] = center1;
}

// Configure appearance
void volumeVisuals( int id, vec3 localPos, out vec3 color, out float baseOpacity, out float falloff )
{
    if (id == VOL_1)
    {
        vec3 black = vec3(0.0);
        vec3 white = vec3(1.0);
        vec3 gradientColor = mix(black, white,localPos.x);

        color = gradientColor;
        baseOpacity = 2.0;
        falloff = 99.0;
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

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    vec3 hitWorld = worldPos(vec3(texCoord, depthSample)) - BlockPosition;
    float maxDist = length(hitWorld - ro);

    for (int i = 0; i < STEPS; i++)
    {
        vec3 p = ro + rd * ray;
        computeSDFs(p);

        // dynamic marching
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
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;

    vec3 ro = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3 hitWorld = worldPos(vec3(texCoord, depthSample)) - BlockPosition;
    vec3 rd = normalize(hitWorld - ro);

    vec4 volume = raymarchVolume(ro, rd);

    vec3 finalColor = original * (1.0 - volume.a) + volume.rgb;
    fragColor = vec4(finalColor, 1.0);
}