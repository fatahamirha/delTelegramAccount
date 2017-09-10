package org.telegram.android.core.engines;

import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.service.TLLocalActionEncryptedCancelled;
import org.telegram.android.core.model.service.TLLocalActionEncryptedCreated;
import org.telegram.android.core.model.service.TLLocalActionEncryptedRequested;
import org.telegram.android.core.model.service.TLLocalActionEncryptedWaiting;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.StreamingUtils;

/**
 * Created by ex3ndr on 03.01.14.
 */
public class SecretEngine {
    private static final String TAG = "SecretEngine";

    private SecretDatabase secretDatabase;
    private ModelEngine engine;
    private TelegramApplication application;

    public SecretEngine(ModelEngine engine) {
        this.engine = engine;
        this.application = engine.getApplication();
        this.secretDatabase = new SecretDatabase(engine);
    }

    public EncryptedChat[] getPendingEncryptedChats() {
        return secretDatabase.getPendingEncryptedChats();
    }

    public void deleteEncryptedChat(int chatId) {
        secretDatabase.deleteChat(chatId);
    }

    public EncryptedChat getEncryptedChat(int chatId) {
        return secretDatabase.loadChat(chatId);
    }

    public EncryptedChat[] getEncryptedChats(int[] chatIds) {
        return secretDatabase.loadChats(chatIds);
    }

    public EncryptedChat[] getEncryptedChats(Integer[] chatIds) {
        return secretDatabase.loadChats(chatIds);
    }

    public void setSelfDestructTimer(int chatId, int time) {
        EncryptedChat chat = getEncryptedChat(chatId);
        chat.setSelfDestructTime(time);
        secretDatabase.updateChat(chat);
    }

    public void updateEncryptedChatKey(TLAbsEncryptedChat chat, byte[] key) {
        int id = chat.getId();
        EncryptedChat encryptedChat = secretDatabase.loadChat(chat.getId());
        if (encryptedChat != null) {
            encryptedChat.setKey(key);
            secretDatabase.updateChat(encryptedChat);
        }
    }

    public EncryptedChat loadChat(int id) {
        return secretDatabase.loadChat(id);
    }

    public void updateEncryptedChat(TLAbsEncryptedChat chat) {
        int id = chat.getId();

        if (id == 0) {
            Logger.w(TAG, "Ignoring encrypted chat");
            return;
        }

        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);

        if (chat instanceof TLEncryptedChat) {
            date = ((TLEncryptedChat) chat).getDate();
        } else if (chat instanceof TLEncryptedChatRequested) {
            date = ((TLEncryptedChatRequested) chat).getDate();
        } else if (chat instanceof TLEncryptedChatWaiting) {
            date = ((TLEncryptedChatWaiting) chat).getDate();
        }

        EncryptedChat encryptedChat = secretDatabase.loadChat(chat.getId());
        if (encryptedChat != null) {
            if (!(chat instanceof TLEncryptedChat) && !(chat instanceof TLEncryptedChatDiscarded)) {
                return;
            }
            writeEncryptedChatInfo(encryptedChat, chat);
            secretDatabase.updateChat(encryptedChat);
        } else {
            if (!(chat instanceof TLEncryptedChatWaiting) && !(chat instanceof TLEncryptedChatRequested)) {
                return;
            }
            encryptedChat = new EncryptedChat();
            encryptedChat.setId(id);
            writeEncryptedChatInfo(encryptedChat, chat);
            secretDatabase.createChat(encryptedChat);
        }
        if (application.getEncryptedChatSource() != null) {
            application.getEncryptedChatSource().notifyChatChanged(id);
        }

        DialogDescription description = engine.getDescriptionForPeer(PeerType.PEER_USER_ENCRYPTED, encryptedChat.getId());
        if (description == null) {
            description = new DialogDescription(PeerType.PEER_USER_ENCRYPTED, encryptedChat.getId());
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            engine.getDialogsEngine().updateOrCreateDialog(description);

            if (encryptedChat.getState() == EncryptedChatState.REQUESTED) {
                application.getNotifications().onNewSecretChatRequested(encryptedChat.getUserId(), encryptedChat.getId());
            }
        } else {
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.DISCARDED:
                    description.setMessage("Chat discarded");
                    description.setExtras(new TLLocalActionEncryptedCancelled());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            engine.getDialogsEngine().updateOrCreateDialog(description);

            if (encryptedChat.getState() == EncryptedChatState.NORMAL) {
                application.getNotifications().onNewSecretChatEstablished(encryptedChat.getUserId(), encryptedChat.getId());
            } else if (encryptedChat.getState() == EncryptedChatState.DISCARDED) {
                application.getNotifications().onNewSecretChatCancelled(encryptedChat.getUserId(), encryptedChat.getId());
            }
        }
    }

    private void writeEncryptedChatInfo(EncryptedChat chat, TLAbsEncryptedChat rawChat) {
        if (rawChat instanceof TLEncryptedChatRequested) {
            TLEncryptedChatRequested requested = (TLEncryptedChatRequested) rawChat;
            chat.setAccessHash(requested.getAccessHash());
            chat.setUserId(requested.getAdminId());
            byte[] tmpKey = CryptoUtils.concat(
                    StreamingUtils.intToBytes(requested.getGA().getLength()),
                    requested.getGA().cleanData());
            chat.setKey(tmpKey);
            chat.setState(EncryptedChatState.REQUESTED);
            chat.setOut(false);
        } else if (rawChat instanceof TLEncryptedChatWaiting) {
            TLEncryptedChatWaiting waiting = (TLEncryptedChatWaiting) rawChat;
            chat.setAccessHash(waiting.getAccessHash());
            chat.setUserId(waiting.getParticipantId());
            chat.setState(EncryptedChatState.WAITING);
            chat.setOut(true);
        } else if (rawChat instanceof TLEncryptedChatDiscarded) {
            chat.setState(EncryptedChatState.DISCARDED);
        } else if (rawChat instanceof TLEncryptedChat) {
            chat.setState(EncryptedChatState.NORMAL);
        }
    }

    public void saveSecretChats(EncryptedChat[] chats) {
        secretDatabase.updateOrCreateChats(chats);
    }

    public void clear() {
        secretDatabase.clear();
    }
}
