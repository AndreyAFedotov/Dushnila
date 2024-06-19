package com.iceekb.dushnila.jpa.converter;

import com.iceekb.dushnila.jpa.enums.ChannelApproved;
import jakarta.persistence.AttributeConverter;

public class ChannelApprovedConverter implements AttributeConverter<ChannelApproved, Character> {
    @Override
    public Character convertToDatabaseColumn(ChannelApproved channelApproved) {
        return channelApproved.getId();
    }

    @Override
    public ChannelApproved convertToEntityAttribute(Character character) {
        return ChannelApproved.of(character);
    }
}
