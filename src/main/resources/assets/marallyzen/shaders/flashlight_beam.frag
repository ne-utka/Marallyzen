#version 150

in vec4 vertexColor;
in float vertexDistance;
in vec2 vertexUv;

uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

void main() {
    // Base beam color (white with intensity)
    vec3 beamColor = vertexColor.rgb;
    
    // Radial falloff (bright center, soft edges)
    // Using distance from center of beam cross-section
    float radial = 1.0 - length(vertexUv - 0.5) * 2.0;
    radial = max(0.0, radial);
    radial = pow(radial, 0.5); // Soft falloff
    
    // Distance falloff (exponential decay)
    float distanceFade = exp(-vertexDistance * 0.08);
    
    // Intensity multiplier for searchlight brightness
    float intensity = radial * distanceFade * 4.0;
    
    // Apply intensity
    vec3 finalColor = beamColor * intensity;
    
    // Fog interaction - blend with fog color
    float fogFactor = smoothstep(FogStart, FogEnd, vertexDistance);
    finalColor = mix(finalColor, FogColor.rgb, fogFactor * 0.4);
    
    // Alpha based on intensity and distance
    float alpha = vertexColor.a * intensity * (1.0 - fogFactor * 0.3);
    
    fragColor = vec4(finalColor, alpha);
}





















