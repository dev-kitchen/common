package com.linkedout.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceRequestData<T> implements Serializable {
	private String requestMethod;
	private T data;
	private String responseRoutingKey;
}
