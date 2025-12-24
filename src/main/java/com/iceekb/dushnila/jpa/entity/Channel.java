package com.iceekb.dushnila.jpa.entity;

import com.iceekb.dushnila.message.util.ChannelApprovedConverter;
import com.iceekb.dushnila.message.enums.ChannelApproved;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "channels")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tg_id")
    private Long tgId;

    @Column(name = "chat_name")
    private String chatName;

    @Column(name = "first_message")
    private LocalDateTime firstMessage;

    @Column(name = "last_message")
    private LocalDateTime lastMessage;

    @Column(name = "message_count")
    private Long messageCount;

    @Column(name = "approved")
    @Convert(converter = ChannelApprovedConverter.class)
    private ChannelApproved approved;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Channel channel)) return false;
        return Objects.equals(id, channel.id) && Objects.equals(tgId, channel.tgId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tgId);
    }
}
