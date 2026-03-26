package com.foodstreet.voice.dto.projection;

public interface GeofenceMatchProjection {
    Long getId();
    String getName();
    String getDescription();
    String getAddress();
    String getAudioUrl();
    Integer getTriggerRadius();
    Integer getPriority();
    Double getLatitude();
    Double getLongitude();
    Double getDistance();
    String getLocalizationStatus();
}
