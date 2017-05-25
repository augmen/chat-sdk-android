/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package wanderingdevelopment.tk.sdkbaseui.adapter;

import co.chatsdk.ui.chat.ChatSDKMessagesListAdapter;
import co.chatsdk.core.dao.DaoCore;

import java.util.Comparator;

import static co.chatsdk.ui.chat.ChatSDKMessagesListAdapter.MessageListItem;

public class MessageItemSorter implements Comparator<ChatSDKMessagesListAdapter.MessageListItem> {
    public static final int ORDER_TYPE_ASC = DaoCore.ORDER_ASC;
    public static final int ORDER_TYPE_DESC = DaoCore.ORDER_DESC;

    private int order = ORDER_TYPE_DESC;

    public MessageItemSorter(){}

    public MessageItemSorter(int order) {
        this.order = order;
    }

    @Override
    public int compare(MessageListItem x, MessageListItem y) {

        if (order == ORDER_TYPE_ASC)
            return (x == null || y == null) ? -1 : x.getTimeInMillis() > y.getTimeInMillis() ? -1 : 1;
        else return (x == null || y == null) ? 1 : x.getTimeInMillis() > y.getTimeInMillis()? 1 : -1 ;
    }
}
