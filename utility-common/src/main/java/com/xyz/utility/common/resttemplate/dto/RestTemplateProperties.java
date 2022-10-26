package com.xyz.utility.common.resttemplate.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RestTemplateProperties {
    private int connectTimeout;
    private int readTimeout;
    private int maxConnection;
    private boolean systemProxyEnable;
}
