#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out float vertexDistance;
out vec2 vertexUv;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    
    // Calculate distance from camera for fog
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vertexDistance = length(viewPos.xyz);
    
    // Pass color and UV (for radial falloff calculation)
    vertexColor = Color;
    
    // Calculate UV for radial falloff
    // We'll use the vertex position relative to beam center
    // This will be calculated in the geometry generation
    vertexUv = vec2(0.0, 0.0);
}





















