#version 330

uniform sampler2D DiffuseSampler0;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 original = texture(DiffuseSampler0, texCoord).rgb;
    vec3 tinted = original * vec3(0.4, 0.6, 1.5);
    fragColor = vec4(clamp(tinted, 0.0, 1.0), 1.0);
}
