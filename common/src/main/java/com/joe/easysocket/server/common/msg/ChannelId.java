package com.joe.easysocket.server.common.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @author joe
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelId {
    @NonNull
    private String channel;
    @NonNull
    private String balanceId;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ChannelId)) {
            return false;
        }

        ChannelId id = (ChannelId) obj;
        return id.getBalanceId().equals(balanceId) && id.getChannel().equals(channel);
    }
}
