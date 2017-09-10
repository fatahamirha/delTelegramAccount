package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.config.NotificationSettings;
import org.telegram.android.core.model.EncryptedChat;
import org.telegram.android.core.model.EncryptedChatState;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.service.TLLocalActionEncryptedTtl;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLDecryptedMessageActionSetMessageTTL;
import org.telegram.api.TLDecryptedMessageService;
import org.telegram.api.TLInputEncryptedChat;
import org.telegram.api.requests.TLRequestMessagesSendEncryptedService;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.TLBytes;

import java.io.IOException;

import static org.telegram.mtproto.secure.CryptoUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 16.10.13
 * Time: 16:28
 */
public class EncryptedChatInfoFragment extends TelegramFragment {

    private static final String TAG = "EncryptedInfoFragment";

    private static final int PICK_NOTIFICATION_SOUND = 1;

    private int chatId;

    private TextView nameView;
    private TextView onlineView;
    private AvatarView avatarView;
    private TextView phoneView;
    private ImageView enabledView;
    private View notifications;
    private View notificationsSound;
    private TextView notificationSoundTitle;
    private TextView mediaCounter;
    private View notificationsLed;
    private TextView notificationLedTitle;

    private View encryptionKeyButton;
    private TextView encryptionKeyTitle;
    private ImageView encryptionIcon;

    private View selfDestructTimerButton;
    private TextView selfDestructTimerTitle;
    private TextView selfDestructTimerValue;

    private EncryptedChat chat;
    private User user;

    public EncryptedChatInfoFragment() {

    }

    public EncryptedChatInfoFragment(int chatId) {
        this.chatId = chatId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = wrap(inflater).inflate(R.layout.secret_fragment, container, false);
        avatarView = (AvatarView) res.findViewById(R.id.avatar);
        nameView = (TextView) res.findViewById(R.id.name);
        onlineView = (TextView) res.findViewById(R.id.online);
        phoneView = (TextView) res.findViewById(R.id.phone);
        enabledView = (ImageView) res.findViewById(R.id.notificationsCheck);
        notifications = res.findViewById(R.id.notificationsButton);
        notificationsSound = res.findViewById(R.id.notificationSound);
        notificationSoundTitle = (TextView) res.findViewById(R.id.notificationSoundTitle);
        notificationsLed = res.findViewById(R.id.notificationLed);
        notificationLedTitle = (TextView) res.findViewById(R.id.notificationLedTitle);
        mediaCounter = (TextView) res.findViewById(R.id.mediaCoutner);
        encryptionKeyButton = res.findViewById(R.id.encryptionKey);
        encryptionKeyTitle = (TextView) res.findViewById(R.id.encryptionKeyTitle);
        encryptionIcon = (ImageView) res.findViewById(R.id.encryptionIcon);

        selfDestructTimerButton = res.findViewById(R.id.selfDestruct);
        selfDestructTimerTitle = (TextView) res.findViewById(R.id.selfDestructTitle);
        selfDestructTimerValue = (TextView) res.findViewById(R.id.selfDestructValue);

        res.findViewById(R.id.mediaButton).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openMedia(PeerType.PEER_USER_ENCRYPTED, chatId);
            }
        }));
        res.findViewById(R.id.sendMessage).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openDialog(PeerType.PEER_USER, user.getUid());
            }
        }));
        encryptionKeyButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chat.getState() == EncryptedChatState.NORMAL) {
                    getRootController().openKeyPreview(SHA1(chat.getKey()), chat.getUserId());
                }
            }
        }));
        selfDestructTimerButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setItems(new CharSequence[]{"Off", "2s", "5s", "1m", "1h", "1d", "1w"}, secure(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                    default:
                                        setSelfDestruct(0);
                                        return;
                                    case 1:
                                        setSelfDestruct(2);
                                        return;
                                    case 2:
                                        setSelfDestruct(5);
                                        return;
                                    case 3:
                                        setSelfDestruct(60);
                                        return;
                                    case 4:
                                        setSelfDestruct(60 * 60);
                                        return;
                                    case 5:
                                        setSelfDestruct(24 * 60 * 60);
                                        return;
                                    case 6:
                                        setSelfDestruct(7 * 24 * 60 * 60);
                                        return;
                                }
                            }
                        })).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        }));

        chat = application.getEngine().getEncryptedChat(chatId);
        user = application.getEngine().getUser(chat.getUserId());
        bindUi();
        return res;
    }

    private void setSelfDestruct(final int sec) {
        if (chat.getSelfDestructTime() == sec) {
            return;
        }
        runUiTask(new AsyncAction() {

            private long randomId = Entropy.generateRandomId();
            private long randomIntId = Entropy.generateRandomId();

            @Override
            public void execute() throws AsyncException {
                TLDecryptedMessageService service = new TLDecryptedMessageService();
                service.setRandomId(randomIntId);
                service.setRandomBytes(new TLBytes(Entropy.generateSeed(32)));
                service.setAction(new TLDecryptedMessageActionSetMessageTTL(sec));

                byte[] msg;
                try {
                    msg = application.getEncryptionController().encryptMessage(service, chatId);
                } catch (IOException e) {
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                }

                rpc(new TLRequestMessagesSendEncryptedService(
                        new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), randomId,
                        new TLBytes(msg)));

                application.getEngine().getSecretEngine().setSelfDestructTimer(chatId, sec);
                application.getEngine().onNewInternalServiceMessage(
                        PeerType.PEER_USER_ENCRYPTED, chatId,
                        application.getCurrentUid(),
                        (int) (TimeOverlord.getInstance().getServerTime() / 1000),
                        new TLLocalActionEncryptedTtl(sec));
                application.notifyUIUpdate();
            }

            @Override
            public void afterExecute() {
                chat = application.getEngine().getEncryptedChat(chatId);
                bindUi();
            }
        });

    }

    private void bindUi() {

        if (chat.getState() == EncryptedChatState.NORMAL) {
            Bitmap bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            byte[] key = SHA1(chat.getKey());
            int[] colors = new int[]{
                    0xffffffff,
                    0xffd5e6f3,
                    0xff2d5775,
                    0xff2f99c9};
            for (int i = 0; i < 64; i++) {
                int index = (key[i / 4] >> (2 * (i % 4))) & 0x3;
                bitmap.setPixel(i % 8, i / 8, colors[index]);
            }
            encryptionIcon.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 48, 48, false));

            encryptionKeyButton.setEnabled(true);
            encryptionKeyTitle.setTextColor(0xff010101);

            selfDestructTimerButton.setEnabled(true);
            selfDestructTimerTitle.setTextColor(0xff010101);
            if (chat.getSelfDestructTime() <= 0) {
                selfDestructTimerValue.setText("Off");
            } else {
                selfDestructTimerValue.setText(TextUtil.formatHumanReadableDuration(chat.getSelfDestructTime()));
            }

        } else {
            encryptionIcon.setImageDrawable(null);
            encryptionKeyButton.setEnabled(false);
            encryptionKeyTitle.setTextColor(0xff808080);

            selfDestructTimerButton.setEnabled(false);
            selfDestructTimerTitle.setTextColor(0xff808080);
            selfDestructTimerValue.setText("");
        }

        avatarView.setEmptyUser(user.getDisplayName(), user.getUid());
        if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
            final TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) user.getPhoto();
            if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                avatarView.requestAvatar(avatarPhoto.getPreviewLocation());
                if (avatarPhoto.getFullLocation() instanceof TLLocalFileLocation) {
                    avatarView.setOnClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getRootController().openAvatarPreview((TLLocalFileLocation) avatarPhoto.getFullLocation());
                        }
                    }));
                } else {
                    avatarView.setOnClickListener(null);
                }
            } else {
                avatarView.requestAvatar(null);
                avatarView.setOnClickListener(null);
            }
        } else {
            avatarView.requestAvatar(null);
            avatarView.setOnClickListener(null);
        }

        String name = user.getFirstName() + " " + user.getLastName();
        final String phone = user.getPhone();
        int statusValue = getUserState(user.getStatus());
        if (statusValue < 0) {
            onlineView.setText(R.string.st_offline);
            onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
        } else if (statusValue == 0) {
            onlineView.setText(R.string.st_online);
            onlineView.setTextColor(getResources().getColor(R.color.st_blue_bright));
        } else {
            onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
            onlineView.setText(formatLastSeen(statusValue));
        }

        nameView.setText(name);
        if (phone != null && phone.length() > 0 && !phone.equals("null")) {
            phoneView.setText(TextUtil.formatPhone(phone));
            phoneView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    copyToPastebin("+" + phone);
                    Toast.makeText(getActivity(), R.string.st_profile_phone_copied, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        } else {
            phoneView.setText(R.string.st_profile_phone_hidden);
            phoneView.setOnLongClickListener(null);
        }

        int mediaCount = getEngine().getMediaCount(PeerType.PEER_USER_ENCRYPTED, chatId);
        if (mediaCount == 0) {
            mediaCounter.setText(R.string.st_none);
        } else {
            mediaCounter.setText("" + mediaCount);
        }

        if (application.getNotificationSettings().isEnabledForUser(user.getUid())) {
            enabledView.setImageResource(R.drawable.holo_btn_check_on);
        } else {
            enabledView.setImageResource(R.drawable.holo_btn_check_off);
        }

        notifications.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.getNotificationSettings().isEnabledForUser(user.getUid())) {
                    application.getNotificationSettings().disableForUser(user.getUid());
                    enabledView.setImageResource(R.drawable.holo_btn_check_off);
                } else {
                    application.getNotificationSettings().enableForUser(user.getUid());
                    enabledView.setImageResource(R.drawable.holo_btn_check_on);
                }
            }
        }));

        notificationsSound.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getStringSafe(R.string.st_select_tone));
                if (application.getNotificationSettings().getUserNotificationSound(user.getUid()) != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(application.getNotificationSettings().getUserNotificationSound(user.getUid())));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, PICK_NOTIFICATION_SOUND);
            }
        }));

        notificationsLed.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int[] ids = new int[]{
                        NotificationSettings.LED_NONE,
                        NotificationSettings.LED_DEFAULT,
                        NotificationSettings.LED_WHITE,
                        NotificationSettings.LED_RED,
                        NotificationSettings.LED_GREEN,
                        NotificationSettings.LED_BLUE,
                        NotificationSettings.LED_YELLOW,
                        NotificationSettings.LED_ORANGE,
                        NotificationSettings.LED_PINK,
                        NotificationSettings.LED_PURPLE,
                        NotificationSettings.LED_CYAN,
                };
                final CharSequence[] items = new CharSequence[]{
                        getStringSafe(R.string.st_none),
                        getStringSafe(R.string.st_default),
                        getStringSafe(R.string.st_notifications_led_white),
                        getStringSafe(R.string.st_notifications_led_red),
                        getStringSafe(R.string.st_notifications_led_green),
                        getStringSafe(R.string.st_notifications_led_blue),
                        getStringSafe(R.string.st_notifications_led_yellow),
                        getStringSafe(R.string.st_notifications_led_orange),
                        getStringSafe(R.string.st_notifications_led_pink),
                        getStringSafe(R.string.st_notifications_led_purple),
                        getStringSafe(R.string.st_notifications_led_cyan),
                };
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                application.getNotificationSettings().setCustomUserColor(user.getUid(), ids[which]);
                                updateNotificationLed();
                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        }));

        updateNotificationSound();
        updateNotificationLed();
    }

    private void updateNotificationSound() {
        if (notificationSoundTitle != null) {
            String title = application.getNotificationSettings().getUserNotificationSoundTitle(user.getUid());
            if (title == null) {
                notificationSoundTitle.setText(R.string.st_default);
            } else {
                notificationSoundTitle.setText(title);
            }
        }
    }

    private void updateNotificationLed() {
        if (notificationLedTitle != null) {
            notificationLedTitle.setText(getLedModeString(application.getNotificationSettings().getCustomUserColor(user.getUid())));
        }
    }

    private String getLedModeString(int mode) {
        switch (mode) {
            case NotificationSettings.LED_COLORFUL:
            default:
                return getStringSafe(R.string.st_notifications_led_colorful);
            case NotificationSettings.LED_DEFAULT:
                return getStringSafe(R.string.st_default);
            case NotificationSettings.LED_NONE:
                return getStringSafe(R.string.st_none);
            case NotificationSettings.LED_BLUE:
                return getStringSafe(R.string.st_notifications_led_blue);
            case NotificationSettings.LED_CYAN:
                return getStringSafe(R.string.st_notifications_led_cyan);
            case NotificationSettings.LED_GREEN:
                return getStringSafe(R.string.st_notifications_led_green);
            case NotificationSettings.LED_ORANGE:
                return getStringSafe(R.string.st_notifications_led_orange);
            case NotificationSettings.LED_PINK:
                return getStringSafe(R.string.st_notifications_led_pink);
            case NotificationSettings.LED_PURPLE:
                return getStringSafe(R.string.st_notifications_led_purple);
            case NotificationSettings.LED_RED:
                return getStringSafe(R.string.st_notifications_led_red);
            case NotificationSettings.LED_WHITE:
                return getStringSafe(R.string.st_notifications_led_white);
            case NotificationSettings.LED_YELLOW:
                return getStringSafe(R.string.st_notifications_led_yellow);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NOTIFICATION_SOUND && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                String title = ringtone.getTitle(getActivity());
                application.getNotificationSettings().setUserNotificationSound(user.getUid(), uri.toString(), title);
            } else {
                application.getNotificationSettings().setUserNotificationSound(user.getUid(), null, null);
            }
            updateNotificationSound();
        }
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_secret_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }
}
