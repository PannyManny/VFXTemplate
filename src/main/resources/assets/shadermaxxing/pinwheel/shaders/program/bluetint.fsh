#version 330

uniform sampler2D DiffuseSampler0;

in vec2 texCoord;
out vec4 fragColor;

uniform float iTime;
uniform vec3 BlockPosition;
uniform vec3 CameraPosition;
uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;


void main() {
    vec4 transformed = ModelViewMat * vec4(0.0, 0.0, 0.0, 1.0);
    fragColor = vec4(abs(transformed.xyz), 1.0);
}


// works = block position, camera position, iTime
// InverseTransformMatrix = four colors locked to the screen no matter if I move the camera or player
// ModelViewMat = black screen