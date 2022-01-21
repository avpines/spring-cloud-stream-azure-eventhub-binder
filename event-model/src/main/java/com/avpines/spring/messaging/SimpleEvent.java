package com.avpines.spring.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SimpleEvent {

  String id;

  String data;

  @JsonCreator
  public SimpleEvent(
      @JsonProperty("id") String id,
      @JsonProperty("data") String data) {
    this.id = id;
    this.data = data;
  }

}