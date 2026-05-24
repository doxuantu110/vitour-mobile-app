package com.uit.vitour.chat.ui;

import android.os.Build;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.uit.vitour.R;
import com.uit.vitour.chat.model.ChatMessage;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;
    private static final int VIEW_TYPE_TYPING = 3;

    private boolean isTyping = false;
    private String currentStreamingText = "";

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                if (oldItem.getId() == null || newItem.getId() == null) return false;
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                return oldItem.getText().equals(newItem.getText()) && 
                       oldItem.getStatus().equals(newItem.getStatus()) &&
                       oldItem.isOfflineFallback() == newItem.isOfflineFallback();
            }

            @Override
            public Object getChangePayload(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                // If only text changed (streaming), return payload
                if (!oldItem.getText().equals(newItem.getText()) && oldItem.getStatus().equals(newItem.getStatus())) {
                    return "TEXT_UPDATE";
                }
                return super.getChangePayload(oldItem, newItem);
            }
        });
    }

    public void setTypingState(boolean typing, String streamingText, RecyclerView recyclerView) {
        this.currentStreamingText = streamingText;
        
        if (this.isTyping != typing) {
            this.isTyping = typing;
            if (typing) {
                notifyItemInserted(super.getItemCount());
            } else {
                notifyItemRemoved(super.getItemCount());
            }
        } else if (typing && !streamingText.isEmpty()) {
            int typingPos = super.getItemCount();
            if (typingPos >= 0 && typingPos < getItemCount()) {
                // Ensure RecyclerView is attached and not computing layout
                if (recyclerView != null && !recyclerView.isComputingLayout()) {
                    notifyItemChanged(typingPos, "TEXT_UPDATE");
                } else if (recyclerView != null) {
                    recyclerView.post(() -> notifyItemChanged(typingPos, "TEXT_UPDATE"));
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isTyping && position == super.getItemCount()) {
            return VIEW_TYPE_TYPING;
        }
        
        ChatMessage message = getItem(position);
        if ("user".equalsIgnoreCase(message.getRole())) {
            return VIEW_TYPE_USER;
        }
        return VIEW_TYPE_ASSISTANT;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + (isTyping ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_ASSISTANT) {
            View view = inflater.inflate(R.layout.item_ai_message, parent, false);
            return new AiMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_typing, parent, false);
            return new TypingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0).equals("TEXT_UPDATE")) {
            if (holder instanceof AiMessageViewHolder) {
                ((AiMessageViewHolder) holder).updateTextOnly(getItem(position).getText());
            } else if (holder instanceof TypingViewHolder) {
                ((TypingViewHolder) holder).updateStreamText(currentStreamingText);
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(getItem(position));
        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).bind(getItem(position));
        } else if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).updateStreamText(currentStreamingText);
        }
    }

    private String formatTimestamp(long timeInMillis) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeInMillis);
        return DateFormat.format("hh:mm a", cal).toString();
    }

    private CharSequence renderMarkdown(String text) {
        // Basic plain-text to HTML rendering
        String htmlText = text.replace("\n", "<br>")
                              .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                              .replaceAll("\\*(.*?)\\*", "<i>$1</i>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(htmlText);
        }
    }

    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.textViewUserMessage);
            tvTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        void bind(ChatMessage message) {
            if (tvMessage != null) tvMessage.setText(message.getText());
            if (tvTimestamp != null) tvTimestamp.setText(formatTimestamp(message.getTimestamp()));
        }
    }

    class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp, badgeOffline;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.textViewAssistantMessage);
            tvTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            badgeOffline = itemView.findViewById(R.id.badgeOffline);
        }

        void bind(ChatMessage message) {
            if (tvMessage != null) {
                if (ChatMessage.STATUS_ERROR.equals(message.getStatus())) {
                    tvMessage.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                    tvMessage.setText(message.getText());
                } else {
                    tvMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.chat_ai_text));
                    tvMessage.setText(renderMarkdown(message.getText()));
                }
            }
            if (tvTimestamp != null) tvTimestamp.setText(formatTimestamp(message.getTimestamp()));
            
            if (badgeOffline != null) {
                badgeOffline.setVisibility(message.isOfflineFallback() ? View.VISIBLE : View.GONE);
            }
        }

        void updateTextOnly(String text) {
            if (tvMessage != null) {
                // For updates, we parse it too (so if streaming updates old messages it works).
                // Actually during streaming, the message is in the typing indicator.
                // When stream is done, it adds the full AiMessage which gets bound fully via bind()
                tvMessage.setText(renderMarkdown(text));
            }
        }
    }

    class TypingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        
        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
            // Re-using the same ID for stream text if present in item_typing
            tvMessage = itemView.findViewById(R.id.textViewAssistantMessage); 
        }
        
        void updateStreamText(String text) {
            if (tvMessage != null) {
                if (text.isEmpty()) {
                    tvMessage.setText("AI is typing..."); // or animate dots
                } else {
                    // Render raw text during stream to prevent jumping markdown
                    tvMessage.setText(text);
                }
            }
        }
    }
}
