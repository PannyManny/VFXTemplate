#version 330

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Sample the current screen color
    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    // Multiply by a blue tint: keep some red/green so the world is still visible,
    // but push heavily toward blue
    vec3 tinted = original * vec3(0.4, 0.6, 1.5);

    // Clamp so we don't go above 1.0 (pure white would blow out otherwise)
    fragColor = vec4(clamp(tinted, 0.0, 1.0), 1.0);
}
