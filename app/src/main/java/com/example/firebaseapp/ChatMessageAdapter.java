package com.example.firebaseapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import static java.lang.System.load;

/**
 * Created by Anand on 25/11/2016.
 */

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private List<ChatMessage> chatMessages;
    private OnItemClickListener clickListener;

    public ChatMessageAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    public void setClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_message_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ChatMessage chatMessage = chatMessages.get(position);

        if (chatMessage.getPhotoUrl() != null) {
            holder.messageTextView.setVisibility(View.GONE);
            holder.photoImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.photoImageView.getContext())
                    .load(chatMessage.getPhotoUrl())
                    .into(holder.photoImageView);
        } else {
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.photoImageView.setVisibility(View.GONE);
            holder.messageTextView.setText(chatMessage.getMsg());
        }
        holder.nameTextView.setText(chatMessage.getName());
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView photoImageView;
        private TextView messageTextView;
        private TextView nameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.photoImageView = (ImageView) itemView.findViewById(R.id.photoImageView);
            this.messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            this.nameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(clickListener != null) {
                clickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    public void addItem(ChatMessage chatMessage) {
        chatMessages.add(chatMessage);
        notifyDataSetChanged();
    }

    public void clearAll() {
        chatMessages.clear();
        notifyDataSetChanged();
    }
}
