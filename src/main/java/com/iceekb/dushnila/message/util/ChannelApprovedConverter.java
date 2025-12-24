package com.iceekb.dushnila.message.util;

import com.iceekb.dushnila.message.enums.ChannelApproved;
import jakarta.persistence.AttributeConverter;

@SuppressWarnings("ConverterNotAnnotatedInspection")
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
