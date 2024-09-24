package com.microsoft.semantickernel.sample.java.sk.assistant.skills;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Creates URLS for providing directions between two addresses.
 */
public class MapSkill {

    @DefineKernelFunction(description = "Get a url that shows a route between the start and destination address.", name = "planRoute")
    public String planRoute(
            @KernelFunctionParameter(description = "Start address", name = "start")
            String start,
            @KernelFunctionParameter(description = "Destination address", name = "destination")
            String destination
    ) {
        return "https://bing.com/maps/default.aspx?rtp=adr." + URLEncoder.encode(start, StandardCharsets.UTF_8) + "~adr." + URLEncoder.encode(destination, StandardCharsets.UTF_8);
    }
}
