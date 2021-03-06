package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.*;
import android.text.util.Linkify;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.app.ActionBarImpl;
import com.actionbarsherlock.internal.app.ActionBarWrapper;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.coremedia.iso.boxes.TrackReferenceTypeBox;
import com.nineoldandroids.animation.Animator;
import org.telegram.android.base.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.config.DebugSettings;
import org.telegram.android.core.*;
import org.telegram.android.core.audio.VoiceCaptureActor;
import org.telegram.android.core.background.AvatarUploader;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.file.AbsFileSource;
import org.telegram.android.core.model.file.FileSource;
import org.telegram.android.core.model.file.FileUriSource;
import org.telegram.android.core.model.local.TLLocalChatParticipant;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.*;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.*;
import org.telegram.android.util.ReflectionUtiles;
import org.telegram.android.views.*;
import org.telegram.android.views.dialog.ConversationAdapter;
import org.telegram.android.views.dialog.ConversationListView;
import org.telegram.android.views.dialog.VisibleViewItem;
import org.telegram.api.TLAbsInputPeer;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.TLInputUserSelf;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.TLRequestMessagesDeleteChatUser;
import org.telegram.api.requests.TLRequestMessagesDeleteHistory;
import org.telegram.api.requests.TLRequestMessagesDiscardEncryption;
import org.telegram.i18n.I18nUtil;
import org.telegram.notifications.StateSubscriber;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.File;
import java.util.*;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 0:31
 */
public class ConversationFragment extends MediaReceiverFragment implements ViewSourceListener, ChatSourceListener, TypingStates.TypingListener, UserSourceListener, EncryptedChatSourceListener, AvatarUploader.AvatarChatUploadListener,
        StateSubscriber {

    private static final String TAG = "ConversaionFragment";

    private static final int MINIMAL_NEWDIV_COUNT = 10;

    private int peerId;
    private int peerType;

    private ArrayList<MessageWireframe> workingSet;
    private BaseAdapter dialogAdapter;
    private View mainContainer;
    private ConversationListView listView;

    private MessageSource source;

    private View tryAgain;
    private TextView messagesCount;
    private View loadingMore;
    private View loading;
    private View empty;
    private View emptyEncrypted;
    private View deleteButton;
    private TextView waitMessageView;
    private TextView audioTimer;

    private View inputOverlay;

    private EditText editText;
    private ImageButton smileButton;
    private ImageView audioButton;
    private ImageButton sendButton;

    private long viewCreateTime = 0;

    private View contactsPanel;

    private HashMap<Integer, MessageWireframe> selected = new HashMap<Integer, MessageWireframe>();

    private ActionMode actionMode;

    private int selectedIndex = -1;
    private int selectedTop = 0;

    private boolean isEnabledInput = false;

    private boolean isFreshUpdate = true;

    private long firstUnreadMessage = 0;
    private int unreadCount = 0;
    private boolean isAudioButtonVisible = true;
    private boolean isAudioVisible = false;
    private View audioContainer;
    private View messageContainer;
    private View audioSlide;
    private int slideStart;

    private int SLIDE_LIMIT;

    private String audioFile;

    public ConversationFragment(int peerType, int peerId) {
        this.peerId = peerId;
        this.peerType = peerType;
    }

    public ConversationFragment() {

    }

    public int getPeerId() {
        return peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            peerId = savedInstanceState.getInt("peerId");
            peerType = savedInstanceState.getInt("peerType");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
        updateHeader();
        secureCallback(new Runnable() {
            @Override
            public void run() {
                updateHeader();
            }
        });
    }

    private void checkState() {
        if (!application.isLoggedIn()) {
            throw new IllegalStateException("User not logged in");
        }

        if (!application.getKernelsLoader().isLoaded()) {
            throw new IllegalStateException("Application is not loaded");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            peerId = savedInstanceState.getInt("peerId");
            peerType = savedInstanceState.getInt("peerType");
        }
        SLIDE_LIMIT = (int) (UiMeasure.DENSITY * 180);

        checkState();

        viewCreateTime = SystemClock.uptimeMillis();

        View res = wrap(inflater).inflate(R.layout.conv_list, container, false);

        if (peerId == 333000 && peerType == PeerType.PEER_USER) {
            ((TextView) res.findViewById(R.id.emptyLabel)).setText(R.string.st_conv_empty_support);
        }

        listView = (ConversationListView) res.findViewById(R.id.listView);

        // ((ImagingListView) listView).setEnabledDynamicPause(true);

        mainContainer = res.findViewById(R.id.mainContainer);

        if (application.getUserSettings().isWallpaperSet()) {
            if (application.getUserSettings().isWallpaperSolid()) {
                int color = application.getUserSettings().getCurrentWallpaperSolidColor();
                ColorDrawable drawable = new ColorDrawable(color);
                if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    res.setBackgroundDrawable(drawable);
                } else {
                    res.setBackground(drawable);
                }
                listView.setCacheColorHint(color);
            } else {
                Bitmap bitmap = application.getWallpaperHolder().getBitmap();
                if (bitmap != null) {
                    FastBackgroundDrawable drawable = new FastBackgroundDrawable(bitmap);
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        res.setBackgroundDrawable(drawable);
                    } else {
                        res.setBackground(drawable);
                    }
                }
                listView.setCacheColorHint(0);
            }
        } else {
            Bitmap bitmap = application.getWallpaperHolder().getBitmap();
            FastBackgroundDrawable drawable = new FastBackgroundDrawable(bitmap);
            mainContainer.setBackgroundDrawable(drawable);
            listView.setCacheColorHint(0);
        }
        listView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                if (view instanceof BaseMsgView) {
                    ((BaseMsgView) view).unbind();
                }
            }
        });

        contactsPanel = res.findViewById(R.id.contactsPanel);

        audioContainer = res.findViewById(R.id.audioContainer);
        audioTimer = (TextView) res.findViewById(R.id.audioTimer);
        audioSlide = res.findViewById(R.id.audioSlide);
        messageContainer = res.findViewById(R.id.messageContainer);
        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        emptyEncrypted = res.findViewById(R.id.emptyEncrypted);
        empty.setVisibility(View.GONE);
        emptyEncrypted.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);


        inputOverlay = res.findViewById(R.id.panel);

        waitMessageView = (TextView) res.findViewById(R.id.waiting);
        deleteButton = res.findViewById(R.id.delete);

        deleteButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog();
            }
        }));

        View bottomPadding = new View(getActivity());
        bottomPadding.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(8)));

        View topView = View.inflate(getActivity(), R.layout.conv_top, null);
        loadingMore = topView.findViewById(R.id.loadingMore);
        messagesCount = (TextView) topView.findViewById(R.id.messagesCount);
        tryAgain = topView.findViewById(R.id.tryAgain);

        tryAgain.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                source.requestLoadMore(dialogAdapter.getCount());
            }
        }));

        source = application.getDataSourceKernel().getMessageSource(peerType, peerId);
        source.getMessagesSource().onConnected();
        workingSet = source.getMessagesSource().getCurrentWorkingSet();

        dialogAdapter = new MessagesAdapter();

        editText = (EditText) res.findViewById(R.id.text);

        updateImeConfig();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.toString().trim().length() > 0) {
                    application.getSyncKernel().getBackgroundSync().onTyping(peerType, peerId);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (sendButton != null && audioButton != null) {
                    boolean isVisible = editable.toString().trim().length() == 0;
                    if (isVisible) {
                        if (!isAudioButtonVisible) {
                            goneScaled(sendButton);
                            showScaled(audioButton);
                            isAudioButtonVisible = true;
                        }

                    } else {
                        if (isAudioButtonVisible) {
                            goneScaled(audioButton);
                            showScaled(sendButton);
                            isAudioButtonVisible = false;
                        }
                    }
                }
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    doSend();
                    return true;
                }
                if (i == EditorInfo.IME_ACTION_DONE) {
                    doSend();
                    return true;
                }
                if (application.getUserSettings().isSendByEnter()) {
                    if (keyEvent != null && i == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        doSend();
                        return true;
                    }
                }
                return false;
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keycode, KeyEvent keyEvent) {
                if (application.getUserSettings().isSendByEnter()) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_ENTER) {
                        doSend();
                        return true;
                    }
                }
                return false;
            }
        });

        smileButton = (ImageButton) res.findViewById(R.id.smileysButton);
        if (getSmileysController().areSmileysVisible()) {
            smileButton.setImageResource(R.drawable.st_conv_panel_kb);
        } else {
            smileButton.setImageResource(R.drawable.st_conv_panel_smileys_selector);
        }

        smileButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSmileysController().areSmileysVisible()) {
                    smileButton.setImageResource(R.drawable.st_conv_panel_smileys_selector);
                } else {
                    smileButton.setImageResource(R.drawable.st_conv_panel_kb);
                }
                getSmileysController().showSmileys(editText);
            }
        }));
        audioButton = (ImageView) res.findViewById(R.id.startAudio);
        audioButton.setVisibility(View.VISIBLE);
        audioButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isAudioVisible) {
                        showAudio();
                        slideStart = (int) event.getX();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isAudioVisible) {
                        hideAudio(false);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (isAudioVisible) {
                        int slide = slideStart - (int) event.getX();
                        if (slide < 0) {
                            slide = 0;
                        }
                        if (slide > SLIDE_LIMIT) {
                            hideAudio(true);
                        } else {
                            slideAudio(slide);
                        }
                    }
                }
                return true;
            }
        });
        sendButton = (ImageButton) res.findViewById(R.id.sendMessage);
        sendButton.setVisibility(View.GONE);
        sendButton.setEnabled(true);
        animate(sendButton).scaleX(0.0f).scaleY(0.0f).alpha(0.0f).setDuration(0).start();
        audioContainer.setVisibility(View.GONE);
        // animate(audioContainer).translationX(UiMeasure.METRICS.widthPixels).setDuration(0).start();
        sendButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSend();
            }
        }));
        isAudioButtonVisible = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            listView.setLayerType(View.LAYER_TYPE_NONE, null);
        }

        listView.addHeaderView(topView, null, false);
        listView.addFooterView(bottomPadding, null, false);
        listView.setAdapter(dialogAdapter);

        workingSet = source.getMessagesSource().getCurrentWorkingSet();

        updateContactsPanel(true);

        onDataChanged();

        updateOverlayPanel();

        if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            User user = application.getEngine().getUser(chat.getUserId());
            if (chat.isOut()) {
                ((TextView) emptyEncrypted.findViewById(R.id.emptyEncryptedTitle))
                        .setText(getStringSafe(R.string.st_conv_enc_invite_out_title).replace("{name}", user.getFirstName()));
            } else {
                ((TextView) emptyEncrypted.findViewById(R.id.emptyEncryptedTitle))
                        .setText(getStringSafe(R.string.st_conv_enc_invite_in_title).replace("{name}", user.getFirstName()));
            }
        }

        return res;
    }

    private void updateImeConfig() {
        if (application.getUserSettings().isSendByEnter()) {
            editText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        } else {
            editText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setImeOptions(EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        }
    }

    private void deleteDialog() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_delete_header)
                .setMessage(peerType == PeerType.PEER_CHAT ? R.string.st_dialogs_delete_group : R.string.st_dialogs_delete_history)
                .setPositiveButton(R.string.st_yes, secure(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runUiTask(new AsyncAction() {
                            @Override
                            public void execute() throws AsyncException {
                                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                                    try {
                                        rpcRaw(new TLRequestMessagesDiscardEncryption(peerId));
                                    } catch (RpcException e) {
                                        e.printStackTrace();
//                                        if ("ENCRYPTION_ALREADY_DECLINED".equals(e.getErrorTag())) {
//                                            e.printStackTrace();
//                                        } else {
//                                            throw new AsyncException(e);
//                                        }
                                    }
                                    getEngine().deleteHistory(peerType, peerId);
                                    getEngine().getSecretEngine().deleteEncryptedChat(peerId);
                                } else {
                                    final TLAbsInputPeer peer;
                                    if (peerType == PeerType.PEER_CHAT) {
                                        peer = new TLInputPeerChat(peerId);
                                    } else {
                                        User user = application.getEngine().getUser(peerId);
                                        peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
                                    }

                                    int offset = 0;
                                    do {
                                        TLAffectedHistory affectedHistory = rpc(new TLRequestMessagesDeleteHistory(peer, offset));
                                        application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(affectedHistory));
                                        offset = affectedHistory.getOffset();
                                    } while (offset != 0);

                                    if (peerId == PeerType.PEER_CHAT) {
                                        rpc(new TLRequestMessagesDeleteChatUser(peerId, new TLInputUserSelf()));
                                    }

                                    getEngine().deleteHistory(peerType, peerId);
                                }
                                application.notifyUIUpdate();
                            }

                            @Override
                            public void afterExecute() {
                                getRootController().doBack();
                            }
                        });
                    }
                })).setNegativeButton(R.string.st_no, null).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void updateActionMode() {
        if (selected.size() == 1 && actionMode == null) {
            actionMode = getSherlockActivity().startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.conv_actions, menu);
                    if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                        menu.findItem(R.id.forwardMessages).setVisible(false);
                        menu.findItem(R.id.copyMessages).setVisible(false);
                        menu.findItem(R.id.shareMessages).setVisible(false);
                    }

                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    mode.setTitle(I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_selected, selected.size()));
                    if (peerType != PeerType.PEER_USER_ENCRYPTED) {

                        boolean isAllText = true;
                        for (MessageWireframe w : selected.values()) {
                            if (w.message.getRawContentType() != ContentType.MESSAGE_TEXT) {
                                isAllText = false;
                            }
                        }

                        boolean isAllImages = true;
                        for (MessageWireframe w : selected.values()) {
                            if (!(w.message.getExtras() instanceof TLLocalPhoto)) {
                                isAllImages = false;
                            }
                        }

                        boolean isAllVideos = true;
                        for (MessageWireframe w : selected.values()) {
                            if (!(w.message.getExtras() instanceof TLLocalVideo)) {
                                isAllVideos = false;
                            }
                        }

                        boolean isAllDocuments = true;
                        for (MessageWireframe w : selected.values()) {
                            if (!(w.message.getExtras() instanceof TLLocalDocument)) {
                                isAllDocuments = false;
                            }
                        }

                        if (isAllText || isAllImages || isAllVideos || isAllDocuments) {
                            menu.findItem(R.id.shareMessages).setVisible(true);
                        } else {
                            menu.findItem(R.id.shareMessages).setVisible(false);
                        }

                        if (isAllText) {
                            menu.findItem(R.id.copyMessages).setVisible(true);
                        } else {
                            menu.findItem(R.id.copyMessages).setVisible(false);
                        }
                    }
                    return true;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.forwardMessages) {
                        ArrayList<Integer> forwardMids = new ArrayList<Integer>();
                        for (Integer i : selected.keySet()) {
                            ChatMessage message = getEngine().getMessageByDbId(i);
                            if (message != null) {
                                if (message.getState() == MessageState.SENT || message.getState() == MessageState.READED) {
                                    forwardMids.add(message.getMid());
                                }
                            }
                        }
                        mode.finish();

                        if (forwardMids.size() == 0) {
                            Toast.makeText(getActivity(), R.string.st_conv_action_nothing_to_forward, Toast.LENGTH_SHORT).show();
                        } else {
                            getRootController().forwardMessages(forwardMids.toArray(new Integer[0]));
                        }
                        return true;
                    } else if (item.getItemId() == R.id.deleteMessages) {
                        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                .setMessage(getStringSafe(R.string.st_conv_delete_message).replace("{messages}",
                                        I18nUtil.getInstance().getPluralFormatted(R.plurals.st_messages, selected.size())))
                                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        for (Integer i : selected.keySet()) {
                                            ChatMessage message = getEngine().getMessageByDbId(i);
                                            if (message != null) {
                                                if (message.getState() == MessageState.SENT || message.getState() == MessageState.READED) {
                                                    getEngine().deleteSentMessage(i);
                                                } else {
                                                    getEngine().deleteUnsentMessage(i);
                                                }
                                            }
                                        }
                                        application.getSyncKernel().getBackgroundSync().resetDeletionsSync();
                                        application.notifyUIUpdate();
                                        mode.finish();
                                    }
                                })
                                .setNegativeButton(R.string.st_no, null)
                                .create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                        return true;
                    } else if (item.getItemId() == R.id.copyMessages) {
                        MessageWireframe[] messages = selected.values().toArray(new MessageWireframe[0]);
                        Arrays.sort(messages, new Comparator<MessageWireframe>() {
                            @Override
                            public int compare(MessageWireframe lhs, MessageWireframe rhs) {
                                return (int) (lhs.sortingOrder - rhs.sortingOrder);
                            }
                        });

                        String messageLog = buildMessageLog(messages);
                        copyToPastebin(messageLog);
                        if (selected.size() == 1) {
                            Toast.makeText(getActivity(), R.string.st_conv_copied, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), getStringSafe(R.string.st_conv_copied_mult)
                                    .replace("{message}", I18nUtil.getInstance().getPluralFormatted(R.plurals.st_messages, selected.size())), Toast.LENGTH_SHORT).show();
                        }
                        mode.finish();
                        return true;
                    } else if (item.getItemId() == R.id.shareMessages) {
                        MessageWireframe[] messages = selected.values().toArray(new MessageWireframe[0]);
                        Arrays.sort(messages, new Comparator<MessageWireframe>() {
                            @Override
                            public int compare(MessageWireframe lhs, MessageWireframe rhs) {
                                return (int) (lhs.sortingOrder - rhs.sortingOrder);
                            }
                        });

                        boolean isAllText = true;
                        for (MessageWireframe w : messages) {
                            if (w.message.getRawContentType() != ContentType.MESSAGE_TEXT) {
                                isAllText = false;
                            }
                        }

                        boolean isAllImages = true;
                        for (MessageWireframe w : messages) {
                            if (!(w.message.getExtras() instanceof TLLocalPhoto)) {
                                isAllImages = false;
                            }
                        }

                        boolean isAllVideos = true;
                        for (MessageWireframe w : messages) {
                            if (!(w.message.getExtras() instanceof TLLocalVideo)) {
                                isAllVideos = false;
                            }
                        }

                        boolean isAllDocuemnts = true;
                        for (MessageWireframe w : messages) {
                            if (!(w.message.getExtras() instanceof TLLocalDocument)) {
                                isAllDocuemnts = false;
                            }
                        }

                        if (isAllText) {
                            String messageLog = buildMessageLog(messages);
                            startPickerActivity(new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, messageLog));
                            mode.finish();
                        } else if (isAllImages) {
                            if (messages.length == 1) {
                                TLLocalPhoto photo = (TLLocalPhoto) messages[0].message.getExtras();
                                String downloadKey = DownloadManager.getPhotoKey(photo);
                                if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                    shareInternalFile(downloadKey, "image/jpeg");
                                } else {
                                    Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                ArrayList<String> keys = new ArrayList<String>();
                                for (MessageWireframe w : selected.values()) {
                                    TLLocalPhoto photo = (TLLocalPhoto) w.message.getExtras();
                                    String downloadKey = DownloadManager.getPhotoKey(photo);
                                    if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                        keys.add(downloadKey);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                        mode.finish();
                                        return true;
                                    }
                                }
                                shareInternalFiles(keys.toArray(new String[keys.size()]), "image/jpeg");
                            }
                        } else if (isAllVideos) {
                            if (messages.length == 1) {
                                TLLocalVideo video = (TLLocalVideo) messages[0].message.getExtras();
                                String downloadKey = DownloadManager.getVideoKey(video);
                                if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                    shareInternalFile(downloadKey, "video/mp4");
                                } else {
                                    Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                ArrayList<String> keys = new ArrayList<String>();
                                for (MessageWireframe w : selected.values()) {
                                    TLLocalVideo video = (TLLocalVideo) w.message.getExtras();
                                    String downloadKey = DownloadManager.getVideoKey(video);
                                    if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                        keys.add(downloadKey);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                        mode.finish();
                                        return true;
                                    }
                                }
                                shareInternalFiles(keys.toArray(new String[keys.size()]), "video/mp4");
                            }
                        } else if (isAllDocuemnts) {
                            if (messages.length == 1) {
                                TLLocalDocument doc = (TLLocalDocument) messages[0].message.getExtras();
                                String downloadKey = DownloadManager.getDocumentKey(doc);
                                if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                    shareInternalFile(downloadKey);
                                } else {
                                    Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                ArrayList<String> keys = new ArrayList<String>();
                                for (MessageWireframe w : selected.values()) {
                                    TLLocalDocument doc = (TLLocalDocument) w.message.getExtras();
                                    String downloadKey = DownloadManager.getDocumentKey(doc);
                                    if (application.getDownloadManager().getState(downloadKey) == DownloadState.COMPLETED) {
                                        keys.add(downloadKey);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.st_error_media_not_downloaded, Toast.LENGTH_SHORT).show();
                                        mode.finish();
                                        return true;
                                    }
                                }
                                shareInternalFiles(keys.toArray(new String[keys.size()]));
                            }
                        }

                        mode.finish();
                        return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    actionMode = null;
                    selected.clear();
                    if (dialogAdapter != null) {
                        dialogAdapter.notifyDataSetChanged();
                    }
                }
            });
        } else if (selected.size() == 0 && actionMode != null) {
            actionMode.finish();
            actionMode = null;
        } else if (actionMode != null) {
            actionMode.invalidate();
        }
    }

    protected void updateContactsPanel(boolean initial) {
        if (contactsPanel == null) {
            return;
        }
        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                goneView(contactsPanel, false);
                contactsPanel.setOnClickListener(null);
            } else {
                int linkType = application.getEngine().getUser(peerId).getLinkType();
                if (linkType == LinkType.FOREIGN || linkType == LinkType.REQUEST) {
                    if (linkType == LinkType.REQUEST) {
                        showView(contactsPanel, !initial);
                        ((TextView) contactsPanel.findViewById(R.id.panelTitle)).setText(R.string.st_conv_add_contact);
                        contactsPanel.setOnClickListener(secure(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getRootController().addContact(peerId);
                            }
                        }));
                    } else {
                        if (application.getNotificationSettings().isAddToContactVisible(peerId)) {
                            showView(contactsPanel, !initial);
                            ((TextView) contactsPanel.findViewById(R.id.panelTitle)).setText(R.string.st_conv_share_info);
                            contactsPanel.setOnClickListener(secure(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    application.getNotificationSettings().hideAddToContact(peerId);
                                    application.getEngine().shareContact(peerType, peerId, application.getCurrentUid());
                                    application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                                    application.notifyUIUpdate();
                                    updateContactsPanel(false);
                                }
                            }));
                        } else {
                            goneView(contactsPanel, !initial);
                            contactsPanel.setOnClickListener(null);
                        }
                    }
                } else {
                    goneView(contactsPanel, !initial);
                    contactsPanel.setOnClickListener(null);
                }
            }
        } else {
            goneView(contactsPanel, !initial);
            contactsPanel.setOnClickListener(null);
        }
    }

    private String buildMessageLog(MessageWireframe[] messages) {
        String messageLog = "";
        if (selected.size() == 1) {
            messageLog = messages[0].message.getMessage();
        } else {
            boolean fromSingleUser = true;
            int singleUserUid = messages[0].senderUser.getUid();
            for (MessageWireframe w : messages) {
                if (w.senderUser.getUid() != singleUserUid) {
                    fromSingleUser = false;
                    break;
                }
            }
            for (MessageWireframe w : messages) {
                if (fromSingleUser) {
                    messageLog += w.message.getMessage() + "\n";
                } else {
                    messageLog += w.senderUser.getDisplayName() + ": " + w.message.getMessage() + "\n";
                }
            }
        }
        return messageLog;
    }

    protected void doSend() {
        if (editText == null) {
            return;
        }
        String message = editText.getText().toString().trim();
        if (message.length() == 0) {
            return;
        }
        application.getMessageSender().postTextMessage(peerType, peerId, message);
        application.getSyncKernel().getBackgroundSync().resetTypingDelay();
        dialogAdapter.notifyDataSetChanged();
        listView.setSelectionFromTop(workingSet.size(), -10000);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelectionFromTop(workingSet.size(), -10000);
            }
        });
        editText.setText("");
        application.getTextSaver().clearText(peerType, peerId);
    }

    protected Spanned fixedHtml(String src) {
        Spanned html = Html.fromHtml(src);
        Spannable res = new SpannableString(html.toString());
        for (StyleSpan span : html.getSpans(0, res.length(), StyleSpan.class)) {
            if (span.getStyle() == Typeface.BOLD) {
                bold(res, html.getSpanStart(span), html.getSpanEnd(span));
            }
        }
        for (URLSpan span : html.getSpans(0, res.length(), URLSpan.class)) {
            int uid = Integer.parseInt(span.getURL().substring(1));
            userSpan(res, html.getSpanStart(span), html.getSpanEnd(span), uid);
        }
        return res;
    }

    protected void italic(Spannable src, int start, int end) {
        src.setSpan(new StelsTypefaceSpan("regular", getActivity(), false), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

    }

    protected void bold(Spannable src, int start, int end) {
        src.setSpan(new StelsTypefaceSpan("regular", getActivity(), true), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void userSpan(Spannable src, int start, int end, final int uid) {
        src.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                view.playSoundEffect(SoundEffectConstants.CLICK);
                getRootController().openUser(uid);
            }

            @Override
            public void updateDrawState(TextPaint ds) {

            }
        }, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    protected void onMessageClick(final MessageWireframe message) {
        if (message.message.getContentType() == ContentType.MESSAGE_CONTACT) {
            final int uid = ((TLLocalContact) message.message.getExtras()).getUserId();
            if (uid > 0) {
                getRootController().openDialog(PeerType.PEER_USER, uid);
            }
//            if (application.getEngine().getUidContact(uid) != null || uid == application.getCurrentUid()) {
//                getRootController().openUser(uid);
//            } else {
//                new AlertDialog.Builder(getActivity())
//                        .setTitle(R.string.st_conv_action_add_header)
//                        .setMessage(R.string.st_conv_action_add)
//                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                getRootController().addContact(uid);
//                            }
//                        })
//                        .setNegativeButton(R.string.st_no, null)
//                        .show();
//            }
            return;
        }

        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        final ArrayList<Runnable> actions = new ArrayList<Runnable>();

        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            if (message.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
                items.add(getStringSafe(R.string.st_conv_action_copy));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        copyToPastebin(message.message.getMessage());
                        Toast.makeText(getActivity(), R.string.st_conv_copied, Toast.LENGTH_SHORT).show();
                    }
                });
                if (isEnabledInput) {
                    items.add(getStringSafe(R.string.st_conv_action_quote));
                    actions.add(new Runnable() {
                        @Override
                        public void run() {
                            if (!isEnabledInput) {
                                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            editText.getText().append("\"" + message.message.getMessage() + "\" ");
                            showKeyboard(editText);
                        }
                    });
                }
            }
        }

        if (message.message.getState() != MessageState.PENDING && message.message.getState() != MessageState.FAILURE && peerType != PeerType.PEER_USER_ENCRYPTED) {
            if (isEnabledInput) {
                items.add(getStringSafe(R.string.st_conv_action_forward));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!isEnabledInput) {
                            Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getRootController().forwardMessage(message.message.getMid());
                    }
                });
            }
        }

        if (message.message.getState() == MessageState.FAILURE) {
            items.add(getStringSafe(R.string.st_conv_action_delete));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(getStringSafe(R.string.st_conv_delete_message).replace("{messages}",
                                    I18nUtil.getInstance().getPluralFormatted(R.plurals.st_messages, 1)))
                            .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    application.getEngine().deleteUnsentMessage(message.message.getDatabaseId());
                                    application.notifyUIUpdate();
                                }
                            })
                            .setNegativeButton(R.string.st_no, null)
                            .create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
            });
        }

        if (message.message.getState() == MessageState.SENT || message.message.getState() == MessageState.READED) {
            items.add(getStringSafe(R.string.st_conv_action_delete));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(getStringSafe(R.string.st_conv_delete_message).replace("{messages}",
                                    I18nUtil.getInstance().getPluralFormatted(R.plurals.st_messages, 1)))
                            .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    application.getEngine().deleteSentMessage(message.message.getDatabaseId());
                                    application.getSyncKernel().getBackgroundSync().resetDeletionsSync();
                                    application.notifyUIUpdate();
                                }
                            })
                            .setNegativeButton(R.string.st_no, null)
                            .create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
            });
        }

        if (message.message.isForwarded() && message.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
            final User forwarded = getEngine().getUser(message.message.getForwardSenderId());
            items.add(forwarded.getDisplayName());
            actions.add(new Runnable() {
                @Override
                public void run() {
                    getRootController().openUser(forwarded.getUid());
                }
            });
        }

        if (message.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
            final TLLocalContact contact = (TLLocalContact) message.message.getExtras();
            items.add(contact.getFirstName() + " " + contact.getLastName());
            actions.add(new Runnable() {
                @Override
                public void run() {
                    getRootController().openUser(contact.getUserId());
                }
            });
        }

        if (message.message.getState() == MessageState.FAILURE && (
                message.message.getRawContentType() == ContentType.MESSAGE_TEXT ||
                        message.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                        message.message.isForwarded())) {
            items.add(getStringSafe(R.string.st_conv_action_try_again));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().tryAgain(message.message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            });
        }

        if (message.message.getState() == MessageState.FAILURE &&
                (message.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                        message.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                        message.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT ||
                        message.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED ||
                        message.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW) && !(message.message.isForwarded())) {
            items.add(getStringSafe(R.string.st_conv_action_try_again));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().tryAgainMedia(message.message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            });
        }

        // Add links
        Spannable text = new SpannableString(message.message.getMessage());
        if (Linkify.addLinks(text, Linkify.ALL)) {
            for (final URLSpan span : text.getSpans(0, text.length(), URLSpan.class)) {
                try {
                    final Uri uri = Uri.parse(span.getURL());

                    items.add(span.getURL());

                    actions.add(new Runnable() {
                        @Override
                        public void run() {
                            openUri(uri);
                        }
                    });
                } catch (Exception e) {
                    // Just ignore
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_action_title).setItems(items.toArray(new CharSequence[items.size()]),
                secure(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i >= 0 && i < actions.size()) {
                            actions.get(i).run();
                        }
                    }
                })
        ).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    protected void onPhotoArrived(final String fileName, final int width, final int height, int requestId) {
        if (fileName == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().sendPhoto(peerType, peerId, width, height, fileName);
                    application.notifyUIUpdate();
                }
            }, 300);
        } else {
            application.getEngine().sendPhoto(peerType, peerId, width, height, fileName);
            application.notifyUIUpdate();
        }
    }

    @Override
    protected void onPhotoArrived(final Uri uri, final int width, final int height, int requestId) {
        if (Build.VERSION.SDK_INT >= 18) {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().sendPhoto(peerType, peerId, width, height, uri);
                    application.notifyUIUpdate();
                }
            }, 300);
        } else {
            application.getEngine().sendPhoto(peerType, peerId, width, height, uri);
            application.notifyUIUpdate();
        }
    }

    @Override
    protected void onVideoArrived(final String fileName, int requestId) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                VideoOptimizer.VideoMetadata metadata;
                try {
                    metadata = VideoOptimizer.getVideoSize(fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                }
                application.getEngine().sendVideo(peerType, peerId,
                        new TLUploadingVideo(fileName, metadata.getWidth(), metadata.getHeight()));
            }

            @Override
            public void afterExecute() {
                application.notifyUIUpdate();
            }
        });
    }

    @Override
    protected void onVideoArrived(final Uri uri, int requestId) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                String fileName = getRealPathFromURI(uri);
                VideoOptimizer.VideoMetadata metadata;
                try {
                    metadata = VideoOptimizer.getVideoSize(fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                }

                application.getEngine().sendVideo(peerType, peerId,
                        new TLUploadingVideo(fileName, metadata.getWidth(), metadata.getHeight()));
                application.notifyUIUpdate();
            }

            @Override
            public void afterExecute() {
                application.notifyUIUpdate();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        checkState();

        source.getMessagesSource().addListener(this);
        application.getChatSource().registerListener(this);
        application.getTypingStates().registerListener(this);
        application.getUserSource().registerListener(this);
        application.getEncryptedChatSource().registerListener(this);

        updateImeConfig();

        application.getUiKernel().onOpenedChat(peerType, peerId);

        if (selectedIndex >= 0) {
            listView.setSelectionFromTop(selectedIndex, selectedTop - listView.getPaddingTop());
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelectionFromTop(selectedIndex, selectedTop - listView.getPaddingTop());
                }
            });
        }

//        if (workingSet != source.getMessagesSource().getCurrentWorkingSet()) {
//            onSourceDataChanged();
//        }
        onSourceDataChanged();

        if (getSmileysController().areSmileysVisible()) {
            smileButton.setImageResource(R.drawable.st_conv_panel_kb);
        } else {
            smileButton.setImageResource(R.drawable.st_conv_panel_smileys_selector);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            switch (application.getTechKernel().getDebugSettings().getConversationListLayerType()) {
                default:
                case DebugSettings.LAYER_NONE:
                    listView.setLayerType(View.LAYER_TYPE_NONE, null);
                    break;
                case DebugSettings.LAYER_HARDWARE:
                    listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    break;
                case DebugSettings.LAYER_SOFTWARE:
                    listView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    break;
            }
        }

        String text = application.getTextSaver().getText(peerType, peerId);
        if (text != null && editText != null) {
            editText.setText(text);
            showKeyboard(editText);
        }

        if (peerType == PeerType.PEER_CHAT) {
            application.getSyncKernel().getAvatarUploader().setChatUploadListener(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater
            inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.conv_menu, menu);
        menu.findItem(R.id.attachCamera).setTitle(highlightMenuText(R.string.st_conv_menu_photo));
        menu.findItem(R.id.attachVideo).setTitle(highlightMenuText(R.string.st_conv_menu_video));
        menu.findItem(R.id.attachLocation).setTitle(highlightMenuText(R.string.st_conv_menu_location));
        menu.findItem(R.id.attachDocument).setTitle(highlightMenuText(R.string.st_conv_menu_doc));
        menu.findItem(R.id.attachWebImage).setTitle(highlightMenuText(R.string.st_conv_menu_web));

        MenuItem avatarItem = menu.findItem(R.id.userAvatar);
        View avatarUploadView = avatarItem.getActionView().findViewById(R.id.avatarUploadProgress);
        View avatarUploadProgress = avatarItem.getActionView().findViewById(R.id.uploadProgressBar);
        View avatarUploadError = avatarItem.getActionView().findViewById(R.id.uploadError);
        avatarUploadView.setVisibility(View.GONE);
        avatarUploadProgress.setVisibility(View.GONE);
        avatarUploadError.setVisibility(View.GONE);

        AvatarView imageView = (AvatarView) avatarItem.getActionView().findViewById(R.id.image);
        View touchLayer = avatarItem.getActionView().findViewById(R.id.avatarTouchLayer);
        int padding = 0;//(int) (getPx(1) * (getBarHeight() / ((float) getPx(48))) + 0.5f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getBarHeight() - padding, getBarHeight() - padding, Gravity.TOP | Gravity.RIGHT);
        params.bottomMargin = padding;
        imageView.setLayoutParams(params);
        touchLayer.setLayoutParams(params);
        avatarUploadView.setLayoutParams(params);

        menu.findItem(R.id.userAvatar)
                .getActionView().findViewById(R.id.imageOverlay).setLayoutParams(params);

        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                imageView.setEmptyDrawable(R.drawable.st_support_avatar);
                touchLayer.setOnClickListener(null);
            } else {
                User usr = application.getEngine().getUser(peerId);
                if (usr != null) {
                    imageView.setEmptyUser(usr.getDisplayName(), usr.getUid());
                    if (usr.getPhoto() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto localAvatarPhoto = (TLLocalAvatarPhoto) usr.getPhoto();
                        if (localAvatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestAvatar(localAvatarPhoto.getPreviewLocation());
                        } else {
                            imageView.requestAvatar(null);
                        }
                    } else {
                        imageView.requestAvatar(null);
                    }
                } else {
                    imageView.requestAvatar(null);
                }
                touchLayer.setOnClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(peerId);
                    }
                }));
            }
        } else if (peerType == PeerType.PEER_CHAT) {
            Group group = getEngine().getGroupsEngine().getGroup(peerId);
            imageView.setEmptyGroup(group.getTitle(), group.getChatId());
            int state = application.getSyncKernel().getAvatarUploader().getGroupUploadState(peerId);
            boolean isLoaded = false;
            if (state != AvatarUploader.STATE_NONE) {
                AbsFileSource fileSource = application.getSyncKernel().getAvatarUploader().getGroupUploadingSource(peerId);
                if (fileSource != null) {
                    if (fileSource instanceof FileSource) {
                        imageView.requestRawAvatarSwitch(((FileSource) fileSource).getFileName());
                        showView(avatarUploadView, false);
                        isLoaded = true;
                    } else if (fileSource instanceof FileUriSource) {
                        imageView.requestRawAvatarSwitch(Uri.parse(((FileUriSource) fileSource).getUri()));
                        showView(avatarUploadView, false);
                        isLoaded = true;
                    }
                }
                if (isLoaded) {
                    if (state == AvatarUploader.STATE_ERROR) {
                        showView(avatarUploadError, false);
                        hideView(avatarUploadProgress, false);
                    } else {
                        hideView(avatarUploadError, false);
                        showView(avatarUploadProgress, false);
                    }
                }
            }

            if (!isLoaded) {
                hideView(avatarUploadView, false);
                if (group != null) {
                    if (group.getAvatar() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) group.getAvatar();
                        if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestAvatar(avatarPhoto.getPreviewLocation());
                        } else {
                            imageView.requestAvatar(null);
                        }
                    } else {
                        imageView.requestAvatar(null);
                    }
                } else {
                    imageView.requestAvatar(null);
                }
            }

            touchLayer.setOnClickListener(secure(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getRootController().openChatEdit(peerId);
                }
            }));
        } else {
            final EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat.getUserId() == 333000) {
                imageView.setEmptyDrawable(R.drawable.st_support_avatar);
                touchLayer.setOnClickListener(null);
            } else {
                User usr = application.getEngine().getUser(chat.getUserId());
                if (usr != null) {
                    imageView.setEmptyUser(usr.getDisplayName(), usr.getUid());
                    if (usr.getPhoto() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto localAvatarPhoto = (TLLocalAvatarPhoto) usr.getPhoto();
                        if (localAvatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestAvatar(localAvatarPhoto.getPreviewLocation());
                        } else {
                            imageView.requestAvatar(null);
                        }
                    } else {
                        imageView.requestAvatar(null);
                    }
                } else {
                    imageView.requestAvatar(null);
                }
                touchLayer.setOnClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openSecretChatInfo(chat.getId());
                    }
                }));
            }
        }

        updateHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.attachCamera) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }
            requestPhotoChooser(0, PICK_NONE);
            return true;
        }
        if (item.getItemId() == R.id.attachVideo) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            requestVideo(2);
            return true;
        }

        if (item.getItemId() == R.id.attachLocation) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }
            pickLocation();
            return true;
        }

        if (item.getItemId() == R.id.attachDocument) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            pickFile();
            return true;
        }

        if (item.getItemId() == R.id.attachWebImage) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            requestWebImage(0);
            return true;
        }

        return false;
    }

    @Override
    protected void onFragmentResult(int resultCode, Object data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data instanceof TLLocalGeo) {
                TLLocalGeo point = (TLLocalGeo) data;
                application.getEngine().sendLocation(peerType, peerId, point);
                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                application.notifyUIUpdate();
                editText.setText("");
            } else if (data instanceof String) {
                application.getEngine().sendDocument(peerType, peerId, new TLUploadingDocument((String) data));
                application.notifyUIUpdate();
            }
        }
    }

    private void hideOverlayPanel() {
        editText.setEnabled(true);
        inputOverlay.setVisibility(View.GONE);
        isEnabledInput = true;
    }

    private void showOverlayPanel() {
        if (getSmileysController() != null) {
            if (getSmileysController().areSmileysVisible()) {
                getSmileysController().hideSmileys();
            }
        }
        editText.setEnabled(false);
        inputOverlay.setVisibility(View.VISIBLE);
        isEnabledInput = false;
    }

    private void updateOverlayPanel() {
        if (peerType == PeerType.PEER_CHAT) {
            Group group = application.getEngine().getGroupsEngine().getGroup(peerId);
            FullChatInfo chatInfo = application.getChatSource().getChatInfo(peerId);
            if ((chatInfo != null && chatInfo.getChatInfo().isForbidden()) || (group != null && group.isForbidden())) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else {
                hideOverlayPanel();
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat == null) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else if (chat.getState() == EncryptedChatState.DISCARDED) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else if (chat.getState() != EncryptedChatState.NORMAL) {
                if (chat.getState() == EncryptedChatState.REQUESTED) {
                    waitMessageView.setText(R.string.st_conv_enc_exchanging_keys);
                } else {
                    waitMessageView.setText(html(
                            getStringSafe(R.string.st_conv_enc_waiting)
                                    .replace("{name}", application.getEngine().getUser(chat.getUserId()).getFirstName())
                    ));
                }
                showOverlayPanel();
                deleteButton.setVisibility(View.GONE);
                waitMessageView.setVisibility(View.VISIBLE);
            } else {
                hideOverlayPanel();
            }
        } else {
            hideOverlayPanel();
        }
    }

    private TextView findSubtitleTextView() {
        ActionBar actionBar = getSherlockActivity().getSupportActionBar();
        if (actionBar instanceof ActionBarWrapper) {
            try {
                android.app.ActionBar nActionBar = ReflectionUtiles.reflectField(actionBar, "mActionBar", android.app.ActionBar.class);
                Object mContainerView = ReflectionUtiles.reflectField(nActionBar, "mContainerView");
                View[] childs = ReflectionUtiles.reflectField(mContainerView, "mChildren", View[].class);
                View actionBarView = childs[0];
                return ReflectionUtiles.reflectField(actionBarView, "mSubtitleView", TextView.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void enableTyping() {

    }

    private void disableTyping() {
//        ActionBar actionBar = getSherlockActivity().getSupportActionBar();
//        if (actionBar instanceof ActionBarWrapper) {
//            try {
//                android.app.ActionBar nActionBar = ReflectionUtiles.reflectField(actionBar, "mActionBar", android.app.ActionBar.class);
//                Object mContainerView = ReflectionUtiles.reflectField(nActionBar, "mContainerView");
//                View[] childs = ReflectionUtiles.reflectField(mContainerView, "mChildren", View[].class);
//                View actionBarView = childs[0];
//                TextView subTitleView = ReflectionUtiles.reflectField(actionBarView, "mSubtitleView", TextView.class);
//                subTitleView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        enableTyping();
    }

    private void setSubtitleTyping(int typing) {
        setSubtitleTyping(getStringSafe(typing));
    }

    private void setSubtitleTyping(String typing) {
        final TextView subtitle = findSubtitleTextView();

        SpannableString string = new SpannableString("_" + typing);

        subtitle.getTextSize();

        string.setSpan(new ReplacementSpan() {
            private TypingDrawable drawable = new TypingDrawable();

            @Override
            public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                if (fm != null) {
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), -fm.ascent);
                }
                if (subtitle != null) {
                    drawable.setCallback(new Drawable.Callback() {

                        @Override
                        public void invalidateDrawable(Drawable who) {
                            subtitle.invalidate();
                        }

                        @Override
                        public void scheduleDrawable(Drawable who, Runnable what, long when) {

                        }

                        @Override
                        public void unscheduleDrawable(Drawable who, Runnable what) {

                        }
                    });
                }
                return drawable.getIntrinsicWidth();
            }

            @Override
            public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
                drawable.setBounds((int) x, y - getPx(8), (int) (x + drawable.getIntrinsicWidth()), y);
                drawable.draw(canvas);
            }
        }, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        getSherlockActivity().getSupportActionBar().setSubtitle(string);
        enableTyping();
    }

    private void setSubtitleBright(int subtitle) {
        setSubtitleBright(getStringSafe(subtitle));
    }

    private void setSubtitleBright(String subtitle) {
        getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitle2Text(subtitle));
        disableTyping();
    }

    private void setSubtitleNormal(int subtitle) {
        setSubtitleNormal(getStringSafe(subtitle));
    }

    private void setSubtitleNormal(String subtitle) {
        getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(subtitle));
        disableTyping();
    }

    private void setSubtitleNormalHtml(String subtitle) {
        getSherlockActivity().getSupportActionBar().setSubtitle(html(subtitle));
        disableTyping();
    }


    private void updateHeader() {
        updateContactsPanel(false);
        updateOverlayPanel();
        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText("Telegram"));
                if (application.getTypingStates().isUserTyping(peerId)) {
                    setSubtitleTyping(R.string.lang_common_typing);
                } else {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_support_hint));
                }
            } else {
                User user = application.getEngine().getUser(peerId);
                getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(
                        application.getEmojiProcessor().processEmojiCutMutable(user.getDisplayName(), 0)));

                if (application.getTypingStates().isUserTyping(peerId)) {
                    setSubtitleTyping(R.string.lang_common_typing);
                } else {
                    int status = getUserState(user.getStatus());
                    if (status < 0) {
                        setSubtitleNormal(R.string.st_offline);
                    } else if (status == 0) {
                        setSubtitleBright(R.string.st_online);
                    } else {
                        setSubtitleNormal(formatLastSeen(status));
                    }
                }
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            final EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat.getUserId() == 333000) {
                getSherlockActivity().getSupportActionBar().setTitle(highlightSecureTitleText("Telegram"));
                if (application.getTypingStates().isEncryptedTyping(chat.getId())) {
                    setSubtitleTyping(R.string.lang_common_typing);
                } else {
                    setSubtitleNormal(R.string.st_support_hint);
                }
            } else {
                User user = application.getEngine().getUser(chat.getUserId());
                getSherlockActivity().getSupportActionBar().setTitle(highlightSecureTitleText(user.getDisplayName()));
                if (chat.getState() == EncryptedChatState.DISCARDED) {
                    setSubtitleNormal(R.string.st_conv_enc_cancelled);
                } else {
                    if (application.getTypingStates().isEncryptedTyping(chat.getId())) {
                        setSubtitleTyping(R.string.lang_common_typing);
                    } else {
                        int status = getUserState(user.getStatus());
                        if (status < 0) {
                            setSubtitleNormal(R.string.st_offline);
                        } else if (status == 0) {
                            setSubtitleBright(R.string.st_online);
                        } else {
                            setSubtitleNormal(formatLastSeen(status));
                        }
                    }
                }
            }
        } else {
            Group group = application.getEngine().getGroupsEngine().getGroup(peerId);
            if (group == null)
                return;
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(group.getTitle()));
            FullChatInfo chatInfo = application.getChatSource().getChatInfo(peerId);
            if (group.isForbidden() || (chatInfo != null && chatInfo.getChatInfo().isForbidden())) {
                setSubtitleNormal(R.string.st_conv_removed);
            } else {
                int[] typing = application.getTypingStates().getChatTypes(peerId);
                if (typing.length == 0) {
                    if (chatInfo != null) {
                        int onlineCount = 0;
                        for (TLLocalChatParticipant participant : chatInfo.getChatInfo().getUsers()) {
                            if (getUserState(getEngine().getUser(participant.getUid()).getStatus()) == 0) {
                                onlineCount++;
                            }
                        }
                        if (onlineCount != 0) {
                            String title = "<font_a>" + getQuantityString(R.plurals.st_members,
                                    group.getUsersCount()).replace("{d}",
                                    I18nUtil.getInstance().correctFormatNumber(group.getUsersCount()));
                            title += ",</font_a> <font_b>" + I18nUtil.getInstance().correctFormatNumber(onlineCount) + " " + getStringSafe(R.string.st_online) + "</font_b>";
                            setSubtitleNormalHtml(title);
                        } else {
                            setSubtitleNormal(getQuantityString(R.plurals.st_members,
                                            group.getUsersCount())
                                            .replace("{d}", "" + I18nUtil.getInstance().correctFormatNumber(group.getUsersCount()))
                            );
                        }
                    } else {
                        setSubtitleNormal(getQuantityString(R.plurals.st_members,
                                group.getUsersCount())
                                .replace("{d}", "" + I18nUtil.getInstance().correctFormatNumber(group.getUsersCount())));
                    }
                } else {
                    String[] names = new String[typing.length];
                    for (int i = 0; i < names.length; i++) {
                        names[i] = application.getEngine().getUser(typing[i]).getFirstName();
                    }
                    setSubtitleTyping(TextUtil.formatTyping(names));
                }
            }
        }

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    public void onDataChanged() {
        boolean isLoading = (SystemClock.uptimeMillis() - viewCreateTime < 500);
        if (dialogAdapter.getCount() == 0) {
            if (source.getMessagesSource().getState() == ViewSourceState.COMPLETED) {
                goneView(loading, !isLoading);
                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                    showView(emptyEncrypted, !isLoading);
                } else {
                    showView(empty, !isLoading);
                }
            } else {
                showView(loading, !isLoading);
                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                    emptyEncrypted.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(View.GONE);
                }
            }

            messagesCount.setVisibility(View.GONE);
            tryAgain.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        } else {
            goneView(loading, !isLoading);
            if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                goneView(emptyEncrypted, !isLoading);
            } else {
                goneView(empty, !isLoading);
            }
            showView(listView, !isLoading);

            if (source.getMessagesSource().getState() == ViewSourceState.IN_PROGRESS) {
                loadingMore.setVisibility(View.VISIBLE);
                messagesCount.setVisibility(View.GONE);
                tryAgain.setVisibility(View.GONE);
            } else {
                loadingMore.setVisibility(View.GONE);
                if (source.getMessagesSource().getState() == ViewSourceState.LOAD_MORE_ERROR) {
                    tryAgain.setVisibility(View.VISIBLE);
                    messagesCount.setVisibility(View.GONE);
                } else {
                    tryAgain.setVisibility(View.GONE);
                    messagesCount.setVisibility(View.GONE);

                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("peerId", peerId);
        outState.putInt("peerType", peerType);

        saveListPosition();
        outState.putInt("selectedIndex", selectedIndex);
        outState.putInt("selectedTop", selectedTop);
    }

    @Override
    public void onPause() {
        super.onPause();
        source.getMessagesSource().removeListener(this);
        application.getChatSource().unregisterListener(this);
        application.getTypingStates().unregisterListener(this);
        application.getUserSource().unregisterListener(this);
        application.getEncryptedChatSource().unregisterListener(this);

        application.getUiKernel().onClosedChat(peerType, peerId);

        getNotifications().unregisterSubscriber(this);
        hideAudio(true);

        if (editText != null && editText.getText().toString().trim().length() > 0) {
            application.getTextSaver().saveText(editText.getText().toString().trim(), peerType, peerId);
        } else {
            application.getTextSaver().clearText(peerType, peerId);
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        hideKeyboard(editText);

        isFreshUpdate = true;

        saveListPosition();

        if (peerType == PeerType.PEER_CHAT) {
            application.getSyncKernel().getAvatarUploader().setChatUploadListener(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
        dialogAdapter = null;
        loadingMore = null;
        tryAgain = null;
        messagesCount = null;
        smileButton = null;

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        editText = null;

        sendButton = null;
        empty = null;
        loading = null;
    }

    @Override
    public void onChatChanged(int chatId, DialogDescription description) {
        if (peerType == PeerType.PEER_CHAT && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onChatTypingChanged(int chatId, int[] uids) {
        if (peerType == PeerType.PEER_CHAT && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onUserTypingChanged(int uid, boolean types) {
        if (peerType == PeerType.PEER_USER && peerId == uid) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onEncryptedTypingChanged(int chatId, boolean types) {
        if (peerType == PeerType.PEER_USER_ENCRYPTED && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onEmojiUpdated(boolean completed) {
        listView.post(new Runnable() {
            @Override
            public void run() {
                if (listView != null) {
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        View v = listView.getChildAt(i);
                        if (v != null) {
                            v.invalidate();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onSourceStateChanged() {
        onDataChanged();
    }

    @Override
    public void onSourceDataChanged() {
        long start = SystemClock.uptimeMillis();
        ArrayList<MessageWireframe> nWorkingSet = source.getMessagesSource().getCurrentWorkingSet();
        Logger.d(TAG, "onSourceDataChanged: " + firstUnreadMessage + ", " + isFreshUpdate);
        if (nWorkingSet.size() == 0) {
            workingSet = nWorkingSet;
            dialogAdapter.notifyDataSetChanged();
            onDataChanged();
            return;
        }

        if (isFreshUpdate) {
            DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
            if (description != null && description.getFirstUnreadMessage() != 0 && nWorkingSet.size() > 0) {
                long unreadMessage = description.getFirstUnreadMessage();
                application.getEngine().getDialogsEngine().clearFirstUnreadMessage(peerType, peerId);
                Logger.d(TAG, "Founded first unread message: " + unreadMessage);
                int index = -1;
                for (int i = 0; i < nWorkingSet.size(); i++) {
                    MessageWireframe message = nWorkingSet.get(i);
                    if (peerType != PeerType.PEER_USER_ENCRYPTED) {
                        if (!message.message.isOut() && unreadMessage == message.message.getMid()) {
                            index = i;
                            break;
                        }
                    } else {
                        if (!message.message.isOut() && unreadMessage == message.message.getRandomId()) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index != -1) {
                    firstUnreadMessage = unreadMessage;
                    unreadCount = 0;
                    for (int i = 0; i <= index; i++) {
                        ChatMessage message = nWorkingSet.get(i).message;
                        if (!message.isOut()) {
                            unreadCount++;
                        }
                    }

                    Logger.d(TAG, "Scrolling to first unread message: " + index + ", count: " + unreadCount);

                    if (unreadCount < MINIMAL_NEWDIV_COUNT) {
                        Logger.d(TAG, "Scrolling to first unread message: " + index + ", count: " + unreadCount);
                        firstUnreadMessage = 0;
                        unreadCount = 0;
                    } else {
                        workingSet = nWorkingSet;
                        dialogAdapter.notifyDataSetChanged();
                        //dialogAdapter.notifyDataSetInvalidated();
                        onDataChanged();

                        final int finalIndex = index;
                        listView.setSelectionFromTop(workingSet.size() - finalIndex, getPx(64));
                        listView.post(new Runnable() {
                            @Override
                            public void run() {
                                secureCallbackWeak(new Runnable() {
                                    @Override
                                    public void run() {
                                        listView.setSelectionFromTop(workingSet.size() - finalIndex, getPx(64));
                                    }
                                });
                            }
                        });
                        isFreshUpdate = false;
                        return;
                    }
                } else {
                    Logger.d(TAG, "Unable to find unread message inf working set");
                }
            }
        }

        VisibleViewItem[] oldState = listView.dump();

        Logger.d(TAG, "List states:");
        for (int i = 0; i < oldState.length; i++) {
            Logger.d(TAG, oldState[i].getIndex() + " \t " + oldState[i].getTop() + " \t " + oldState[i].getId());
        }

        int count = listView.getCount();

        if (oldState.length == 0) {
            workingSet = nWorkingSet;
            dialogAdapter.notifyDataSetChanged();
            if (workingSet.size() > 0) {
                listView.setSelectionFromTop(workingSet.size() - 1, -10000);
            }
            onDataChanged();
            return;
        }

        if (workingSet.size() > 0 && nWorkingSet.size() > 0 && listView.getLastVisiblePosition() >= workingSet.size() - 1
                && workingSet.get(0).databaseId != nWorkingSet.get(0).databaseId) {
            // Scrolled to end
            workingSet = nWorkingSet;
            dialogAdapter.notifyDataSetChanged();
            listView.setSelectionFromTop(workingSet.size(), -10000);
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelectionFromTop(workingSet.size(), -10000);
                }
            });
            return;
        }

        int minIndex = oldState[0].getIndex();
        int maxIndex = oldState[oldState.length - 1].getIndex();
        int delta = Math.abs(dialogAdapter.getCount() - count);

        workingSet = nWorkingSet;
        dialogAdapter.notifyDataSetChanged();

        int newIndex = oldState[0].getIndex() + (dialogAdapter.getCount() - count) + 1;
        int newTop = oldState[0].getTop();

        outer:
        for (int i = minIndex - delta; i < maxIndex + delta + 1; i++) {
            if (i < 0)
                continue;
            if (i >= dialogAdapter.getCount())
                break;

            for (int j = 0; j < oldState.length; j++) {
                if (oldState[j].getId() == listView.getItemIdAtPosition(i)) {
                    newIndex = i;
                    newTop = oldState[j].getTop();
                    break;
                }
            }
        }

        listView.setSelectionFromTop(newIndex, newTop);

        onDataChanged();

        Logger.d(TAG, "onSourceDataChanged time: " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    @Override
    public void onEncryptedChatChanged(int chatId, EncryptedChat chat) {
        if (peerType == PeerType.PEER_USER_ENCRYPTED && chatId == peerId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    private void saveListPosition() {
        if (listView != null) {
            int index = listView.getFirstVisiblePosition() + 1;
            View v = listView.getChildAt(1);
            if (v != null) {
                selectedTop = v.getTop();
                selectedIndex = index;
            } else {
                selectedTop = 0;
                selectedIndex = -1;
            }
        }
    }

    @Override
    public void onUsersChanged(User[] users) {

        if (peerType == PeerType.PEER_USER) {
            for (User u : users) {
                if (u.getUid() == peerId) {
                    getSherlockActivity().invalidateOptionsMenu();
                    return;
                }
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat encryptedChat = application.getEngine().getEncryptedChat(peerId);
            if (encryptedChat != null) {
                for (User u : users) {
                    if (u.getUid() == encryptedChat.getUserId()) {
                        getSherlockActivity().invalidateOptionsMenu();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onAvatarUploadingStateChanged(int chatId) {
        if (peerType == PeerType.PEER_CHAT && peerId == chatId) {
            secureCallback(new Runnable() {
                @Override
                public void run() {
                    getSherlockActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    private boolean onBubbleClick(BaseMsgView view, MessageWireframe wireframe) {
        if (selected.size() > 0) {
            if (selected.containsKey(wireframe.message.getDatabaseId())) {
                selected.remove(wireframe.message.getDatabaseId());
            } else {
                selected.put(wireframe.message.getDatabaseId(), wireframe);
            }
            ((BaseMsgView) view).setChecked(selected.containsKey(wireframe.message.getDatabaseId()));
            updateActionMode();
            return true;
        }
        return false;
    }

    @Override
    public void onStateChanged(int kind, long id, int state, Object... args) {
        if (kind == Events.KIND_AUDIO_RECORD) {
            if (state == Events.STATE_ERROR) {
                Logger.d(TAG, "Error: id=" + id);
                Toast.makeText(getActivity(), "Unable to record audio", Toast.LENGTH_SHORT).show();
                hideAudio(true);
            } else if (state == Events.STATE_IN_PROGRESS) {
                Long progress = (Long) args[0];
                Logger.d(TAG, "In progress: " + progress + ", id=" + id);
                audioTimer.setText(TextUtil.formatDuration((int) (progress / 1000)));
            } else if (state == Events.STATE_STOP) {
                Long progress = (Long) args[0];
                Logger.d(TAG, "Stop: " + progress + ", id=" + id);
                if (progress < 1200) {
                    Logger.d(TAG, "Cancel");
                } else {
                    application.getEngine().sendAudio(peerType, peerId,
                            new TLUploadingAudio(audioFile, (int) (progress / 1000)));
//                    application.getEngine().sendDocument(peerType, peerId,
//                            new TLUploadingDocument(audioFile));
                    application.notifyUIUpdate();
                }
            }
        }
    }

    private class MessagesAdapter extends BaseAdapter implements ConversationAdapter {

        @Override
        public int getCount() {
            return workingSet.size();
        }

        @Override
        public MessageWireframe getItem(int i) {
            return workingSet.get(getCount() - i - 1);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).message.getDatabaseId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MessageWireframe message = getItem(i);
            if (view == null) {
                view = newView(application, message, viewGroup);
            }

            boolean showTime = i == 0;
            if (i > 0) {
                MessageWireframe prev = getItem(i - 1);
                showTime = !TextUtil.areSameDays(prev.message.getDate(), message.message.getDate());
            }

            bindView(view, application, message, showTime);

            source.getMessagesSource().onItemsShown(getCount() - i - 1);

            return view;
        }

        @Override
        public int getItemViewType(int position) {
            MessageWireframe msg = getItem(position);

            if (msg.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                return 1;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                return 2;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                return 3;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                return 4;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                return 5;
            }

            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 6;
        }


        public View newView(Context context, MessageWireframe object, ViewGroup parent) {
            if (object.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                View res = View.inflate(context, R.layout.conv_item_system, null);
                ((TextView) res.findViewById(R.id.unreadDivider)).setTypeface(FontController.loadTypeface(context, "medium"));
                return res;
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                return new MessageDocumentView(context);
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                return new MessageAudioView(context);
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN ||
                    object.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW ||
                    object.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                return new MessageMediaView(context);
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                return new MessageContactView(context);
            } else {
                return new MessageView(context);
            }
        }

        public void bindView(View view, Context context, final MessageWireframe object, boolean showTime) {
            if (object.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                TextView timeView = (TextView) view.findViewById(R.id.time);
                if (showTime) {
                    timeView.setText(TextUtil.formatDateLong(object.message.getDate()));
                    timeView.setVisibility(View.VISIBLE);
                } else {
                    timeView.setVisibility(View.GONE);
                }

                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }

                TextView unreadView = (TextView) view.findViewById(R.id.unreadDivider);
                View unreadContainer = view.findViewById(R.id.unreadContainer);

                if (showDiv) {
                    unreadView.setText(I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages, unreadCount));
                    unreadContainer.setVisibility(View.VISIBLE);
                } else {
                    unreadContainer.setVisibility(View.GONE);
                }

                TextView messageView = (TextView) view.findViewById(R.id.message);
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
                AvatarView imageView = (AvatarView) view.findViewById(R.id.image);

                TLAbsLocalAction action = (TLAbsLocalAction) object.message.getExtras();
                User sender = getEngine().getUser(object.message.getSenderId());
                boolean byMyself = sender.getUid() == application.getCurrentUid();
                String senderName = byMyself ? getStringSafe(R.string.st_message_by_you) : sender.getDisplayName();
                String senderHtml = "<b><a href='#" + sender.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(senderName)) + "</a></b>";
                if (action instanceof TLLocalActionUserEditPhoto) {
                    final TLLocalActionUserEditPhoto chatEditPhoto = (TLLocalActionUserEditPhoto) action;
                    messageView.setText(fixedHtml(getStringSafe(R.string.st_message_user_photo_change).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.VISIBLE);
                    if (chatEditPhoto.getPhoto().getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestAvatar(chatEditPhoto.getPhoto().getPreviewLocation());
                    } else {
                        imageView.removeAvatar();
                    }
                    if (chatEditPhoto.getPhoto().getFullLocation() instanceof TLLocalFileLocation) {
                        imageView.setOnClickListener(secure(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getRootController().openAvatarPreview((TLLocalFileLocation) chatEditPhoto.getPhoto().getFullLocation());
                            }
                        }));
                    } else {
                        imageView.setOnClickListener(null);
                    }
                } else if (action instanceof TLLocalActionChatEditPhoto) {
                    final TLLocalActionChatEditPhoto chatEditPhoto = (TLLocalActionChatEditPhoto) action;
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_photo_change_you : R.string.st_message_photo_change).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.VISIBLE);
                    if (chatEditPhoto.getPhoto() instanceof TLLocalAvatarPhoto) {
                        final TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) chatEditPhoto.getPhoto();
                        if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestAvatar(avatarPhoto.getPreviewLocation());
                        } else {
                            imageView.removeAvatar();
                        }
                        if (avatarPhoto.getFullLocation() instanceof TLLocalFileLocation) {
                            imageView.setOnClickListener(secure(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    getRootController().openAvatarPreview((TLLocalFileLocation) avatarPhoto.getFullLocation());
                                }
                            }));
                        } else {
                            imageView.setOnClickListener(secure(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    getRootController().openAvatarPreview((TLLocalFileLocation) avatarPhoto.getPreviewLocation());
                                }
                            }));
                        }
                    } else {
                        imageView.requestAvatar(null);
                        imageView.setOnClickListener(null);
                    }
                } else if (action instanceof TLLocalActionChatCreate) {
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_create_group_you : R.string.st_message_create_group).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatAddUser) {
                    TLLocalActionChatAddUser addUser = (TLLocalActionChatAddUser) action;
                    User added = getEngine().getUser(addUser.getUserId());
                    String addedName = added.getUid() == application.getCurrentUid() ? getStringSafe(R.string.st_message_you) : added.getDisplayName();
                    String addedHtml = "<b><a href='#" + added.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(addedName)) + "</a></b>";
                    messageView.setText(fixedHtml(getStringSafe(addUser.getUserId() == application.getCurrentUid() ? R.string.st_message_added_user_of_you : (byMyself ? R.string.st_message_added_user_you : R.string.st_message_added_user))
                            .replace("{0}", senderHtml)
                            .replace("{1}", addedHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatDeletePhoto) {
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_photo_remove_you : R.string.st_message_photo_remove)
                            .replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatEditTitle) {
                    TLLocalActionChatEditTitle editTitle = (TLLocalActionChatEditTitle) action;
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_name_change_you : R.string.st_message_name_change)
                            .replace("{0}", senderHtml)
                            .replace("{1}", "<b>" + unicodeWrap(TextUtils.htmlEncode(editTitle.getTitle())) + "</b>")));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatDeleteUser) {
                    TLLocalActionChatDeleteUser deleteUser = (TLLocalActionChatDeleteUser) action;
                    if (object.message.getSenderId() == deleteUser.getUserId()) {
                        messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_left_user_you : R.string.st_message_left_user).replace("{0}", senderHtml)));
                    } else {
                        User removed = getEngine().getUser(deleteUser.getUserId());
                        String removedName = removed.getUid() == application.getCurrentUid() ? getStringSafe(R.string.st_message_you) : removed.getDisplayName();
                        String removedHtml = "<b><a href='#" + removed.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(removedName)) + "</a></b>";
                        messageView.setText(fixedHtml(getStringSafe(deleteUser.getUserId() == application.getCurrentUid() ? R.string.st_message_kicked_user_of_you : (byMyself ? R.string.st_message_kicked_user_you : R.string.st_message_kicked_user))
                                .replace("{0}", senderHtml)
                                .replace("{1}", removedHtml)));
                    }
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionEncryptedTtl) {
                    TLLocalActionEncryptedTtl ttl = (TLLocalActionEncryptedTtl) action;
                    if (ttl.getTtlSeconds() > 0) {
                        messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_enc_self_destruct_you : R.string.st_message_enc_self_destruct)
                                .replace("{0}", senderHtml)
                                .replace("{1}", "<b>" + unicodeWrap(TextUtil.formatHumanReadableDuration(((TLLocalActionEncryptedTtl) action).getTtlSeconds())) + "</b>")));
                    } else {
                        messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_enc_self_destruct_off_you : R.string.st_message_enc_self_destruct_off)
                                .replace("{0}", senderHtml)));
                    }

                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionUserRegistered) {
                    messageView.setText(fixedHtml(getStringSafe(R.string.st_message_user_joined_app)
                            .replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else {
                    messageView.setText(R.string.st_message_service);
                    imageView.setVisibility(View.GONE);
                }
            } else {
                bindChatView((BaseMsgView) view, context, object, showTime);
            }
        }

        public void bindChatView(final BaseMsgView view, final Context context, final MessageWireframe object, boolean showTime) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (selected.containsKey(object.message.getDatabaseId())) {
                        selected.remove(object.message.getDatabaseId());
                    } else {
                        selected.put(object.message.getDatabaseId(), object);
                    }
                    ((BaseMsgView) v).setChecked(selected.containsKey(object.message.getDatabaseId()));
                    updateActionMode();
                    return true;
                }
            });

            view.setChecked(selected.containsKey(object.message.getDatabaseId()));

            if (object.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                MessageDocumentView documentView = (MessageDocumentView) view;

                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                documentView.bind(object, showTime, showDiv, unreadCount);

                documentView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!onBubbleClick((BaseMsgView) view, object)) {
                            if (object.message.getExtras() instanceof TLUploadingDocument) {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            })).setNegativeButton(R.string.st_no, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            } else if (object.message.getExtras() instanceof TLLocalDocument) {
                                final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());
                                final TLLocalDocument doc = (TLLocalDocument) object.message.getExtras();
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
//                                    String mimeType = "*/*";
//                                    if (doc.getFileName().indexOf('.') > -1) {
//                                        String ext = doc.getFileName().substring(doc.getFileName().lastIndexOf('.') + 1);
//                                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
//                                    }

                                    openInternalFile(key, doc.getMimeType());
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }
                }));
                documentView.setOnAvatarClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                }));

            } else if (object.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                MessageContactView messageView = (MessageContactView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                messageView.bind(object, showTime, showDiv, unreadCount);

                messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!onBubbleClick((BaseMsgView) view, object)) {
                            onMessageClick(object);
                        }
                    }
                }));
                messageView.setOnAvatarClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                }));
                messageView.setOnAddContactClick(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getRootController().addContact(((TLLocalContact) object.message.getExtras()).getUserId());
                    }
                }));
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
                MessageView messageView = (MessageView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                //long start = SystemClock.uptimeMillis();
                boolean res = messageView.bind(object, showTime, showDiv, unreadCount);
                //Logger.d(TAG, "Bind #" + object.message.getContentType() + " in " + (SystemClock.uptimeMillis() - start) + " ms \t" + res);

                messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!onBubbleClick((BaseMsgView) view, object)) {
                            onMessageClick(object);
                        }
                    }
                }));
                messageView.setOnAvatarClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                }));
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_PHOTO
                    || object.message.getRawContentType() == ContentType.MESSAGE_VIDEO
                    || object.message.getRawContentType() == ContentType.MESSAGE_GEO
                    || object.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN
                    || object.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW
                    || object.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                MessageMediaView messageView = (MessageMediaView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                messageView.bind(object, showTime, showDiv, unreadCount);
                messageView.setOnAvatarClickListener(secure(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                }));

                if (object.message.getExtras() instanceof TLUploadingPhoto || object.message.getExtras() instanceof TLUploadingVideo || object.message.getExtras() instanceof TLUploadingDocument) {
                    messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            })).setNegativeButton(R.string.st_no, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }
                        }
                    }));
                } else if (object.message.getExtras() instanceof TLLocalPhoto) {

                    TLLocalPhoto photo = (TLLocalPhoto) object.message.getExtras();
                    if (!(photo.getFullLocation() instanceof TLLocalFileEmpty)) {
                        final String key = DownloadManager.getPhotoKey(photo);
                        DownloadState state = application.getDownloadManager().getState(key);
                        if (state == DownloadState.FAILURE || state == DownloadState.NONE) {
                            application.getDownloadManager().requestDownload(photo);
                        }

                        messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!onBubbleClick((BaseMsgView) view, object)) {
                                    DownloadState state = application.getDownloadManager().getState(key);
                                    if (state == DownloadState.COMPLETED) {
                                        getRootController().openImage(object.message.getMid(), peerType, peerId);
                                    } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                        // CANCEL
                                        application.getDownloadManager().abortDownload(key);
                                    } else {
                                        // Check
                                        application.getDownloadManager().requestDownload((TLLocalPhoto) object.message.getExtras());
                                    }
                                }
                            }
                        }));
                    } else {
                        messageView.setOnBubbleClickListener(null);
                    }
                } else if (object.message.getExtras() instanceof TLLocalVideo) {
                    final String key = DownloadManager.getVideoKey((TLLocalVideo) object.message.getExtras());
                    messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getFileName(key))), "video/*");
                                    startPickerActivity(intent);
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalVideo video = (TLLocalVideo) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }));
                } else if (object.message.getExtras() instanceof TLLocalGeo) {
                    messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                TLLocalGeo geo = (TLLocalGeo) object.message.getExtras();
                                getRootController().viewLocation(geo.getLatitude(), geo.getLongitude(), object.message.getSenderId());
                            }
                        }
                    }));
                } else if (object.message.getExtras() instanceof TLLocalDocument) {
                    final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());
                    messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    if (((TLLocalDocument) object.message.getExtras()).getMimeType().equals("image/gif")) {
                                        ((MessageMediaView) view).toggleMovie();
                                    } else {
                                        TLLocalDocument doc = (TLLocalDocument) object.message.getExtras();

//                                        String mimeType = "*/*";
//                                        if (doc.getFileName().indexOf('.') > -1) {
//                                            String ext = doc.getFileName().substring(doc.getFileName().lastIndexOf('.') + 1);
//                                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
//                                        }

                                        openInternalFile(key, doc.getMimeType());
                                    }
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }));
                } else {
                    // Unknown
                    messageView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.st_conv_unsupported_title)
                                        .setMessage(R.string.st_conv_unsupported_message)
                                        .setPositiveButton(R.string.st_ok, null)
                                        .create();
                                alertDialog.setCanceledOnTouchOutside(true);
                                alertDialog.show();
                            }
                        }
                    }));
                }
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                MessageAudioView audioView = (MessageAudioView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                audioView.bind(object, showTime, showDiv, unreadCount);

                if (object.message.getExtras() instanceof TLUploadingDocument ||
                        object.message.getExtras() instanceof TLUploadingAudio) {
                    audioView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            })).setNegativeButton(R.string.st_no, secure(new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }
                        }
                    }));
                } else if (object.message.getExtras() instanceof TLLocalDocument) {
                    final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());

                    audioView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    ((MessageAudioView) view).play();
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }));
                } else if (object.message.getExtras() instanceof TLLocalAudio) {
                    final String key = DownloadManager.getAudioKey((TLLocalAudio) object.message.getExtras());

                    DownloadState state = application.getDownloadManager().getState(key);
                    if (state == DownloadState.FAILURE || state == DownloadState.NONE) {
                        application.getDownloadManager().requestDownload((TLLocalAudio) object.message.getExtras());
                    }

                    audioView.setOnBubbleClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!onBubbleClick((BaseMsgView) view, object)) {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    ((MessageAudioView) view).play();
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalAudio video = (TLLocalAudio) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }));
                }
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getItemDate(int id) {
            return getItem(id).message.getDate();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    @Override
    public boolean isParentFragment(TelegramFragment fragment) {
        return false;
    }

    private void slideAudio(int value) {
        animate(audioSlide).translationX(-value).setDuration(0).start();
    }

    private void showAudio() {
        if (isAudioVisible) {
            return;
        }
        isAudioVisible = true;

        audioFile = getUploadTempAudioFile();

        long id = VoiceCaptureActor.LAST_ID.incrementAndGet();
        application.getKernel().getActorKernel().getVoiceCaptureActor().start(id, audioFile);
        getNotifications().unregisterSubscriber(this, Events.KIND_AUDIO_RECORD);
        getNotifications().registerSubscriber(this, Events.KIND_AUDIO_RECORD, id);

        slideAudio(0);
        audioTimer.setText("00:00");

        animate(audioContainer)
                .translationX(UiMeasure.METRICS.widthPixels)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        audioContainer.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                })
                .setDuration(0).start();

        animate(audioContainer)
                .translationX(0)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(160)
                .start();

        animate(messageContainer)
                .translationX(0)
                .setDuration(0)
                .start();
        animate(messageContainer)
                .translationX(-UiMeasure.METRICS.widthPixels)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(160)
                .start();
    }

    private void hideAudio(boolean cancel) {
        if (!isAudioVisible) {
            return;
        }
        isAudioVisible = false;

        application.getKernel().getActorKernel().getVoiceCaptureActor().stop();

        if (cancel) {
            getNotifications().unregisterSubscriber(this);
        }

        animate(audioContainer)
                .translationX(0)
                .setDuration(0)
                .start();
        animate(audioContainer)
                .translationX(UiMeasure.METRICS.widthPixels)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(160)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        audioContainer.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                })
                .start();

        animate(messageContainer)
                .translationX(-UiMeasure.METRICS.widthPixels)
                .setDuration(0)
                .start();
        animate(messageContainer)
                .translationX(0)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(160)
                .start();
    }

    private void goneScaled(final View view) {
        if (view == null) {
            return;
        }

        view.setEnabled(false);
        animate(view).scaleX(0.0f).scaleY(0.0f)
                .alpha(0.0f).setDuration(160).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }).start();
    }

    private void showScaled(final View view) {
        if (view == null) {
            return;
        }

        view.setEnabled(true);
        animate(view).scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(160).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }).start();
    }
}