package org.telegram.android.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.base.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.core.model.file.FileSource;
import org.telegram.android.core.model.file.FileUriSource;
import org.telegram.android.core.model.update.TLLocalCreateChat;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLStatedMessage;
import org.telegram.api.requests.TLRequestMessagesCreateChat;
import org.telegram.tl.TLVector;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 15:01
 */
public class CreateChatCompleteFragment extends MediaReceiverFragment {

    private int[] uids;

    private String imageFileName;
    private Uri imageUri;

    private AvatarView avatarImage;

    private EditText chatTitleView;

    private View mainContainer;

    public CreateChatCompleteFragment(int[] selectedUids) {
        this.uids = selectedUids;
    }

    public CreateChatCompleteFragment() {

    }

    @Override
    protected void onPhotoArrived(String fileName, int width, int height, int requestId) {
        if (cropSupported(Uri.fromFile(new File(fileName)))) {
            requestCrop(Uri.fromFile(new File(fileName)), 200, 200, 0);
        } else {
            imageFileName = fileName;
            imageUri = null;
            avatarImage.requestRawAvatar(fileName);
        }
    }

    @Override
    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {
        if (cropSupported(uri)) {
            requestCrop(uri, 200, 200, 0);
        } else {
            imageFileName = null;
            imageUri = uri;
            avatarImage.requestRawAvatar(imageUri);
        }
    }

    @Override
    protected void onPhotoCropped(Uri uri, int requestId) {
        imageFileName = null;
        imageUri = uri;
        avatarImage.requestRawAvatar(uri);
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        imageFileName = null;
        imageUri = null;
        avatarImage.removeAvatar();
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.create_chat_complete_menu, menu);
        menu.findItem(R.id.done).setTitle(highlightMenuText(R.string.st_new_group_complete_done));
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_new_group_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
            doCreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = wrap(inflater).inflate(R.layout.create_chat_complete, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);

        avatarImage = (AvatarView) res.findViewById(R.id.avatar);
        avatarImage.setEmptyGreyGroup();
        avatarImage.removeAvatar();

        chatTitleView = (EditText) res.findViewById(R.id.title);

        fixEditText(chatTitleView);

        chatTitleView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doCreate();
                    return true;
                }
                return false;
            }
        });

        res.findViewById(R.id.changeAvatar).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri != null || imageFileName != null) {
                    requestPhotoChooser(0, PICK_DEFAULT | PICK_DELETE);
                } else {
                    requestPhotoChooser(0, PICK_DEFAULT);
                }
            }
        }));

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        showKeyboard(chatTitleView);
    }

    private void doCreate() {
        final String chatTitle = chatTitleView.getText().toString().trim();
        if (chatTitle.length() == 0) {
            return;
        }

        runUiTask(new AsyncAction() {

            private TLAbsStatedMessage message;
            private int chatId;

            @Override
            public void execute() throws AsyncException {
                TLVector<TLAbsInputUser> user = new TLVector<TLAbsInputUser>();
                for (int i : uids) {
                    user.add(new TLInputUserContact(i));
                }
                message = rpc(new TLRequestMessagesCreateChat(user, chatTitle));
                application.getUpdateProcessor().onMessage(new TLLocalCreateChat(message));

                TLStatedMessage msg = (TLStatedMessage) message;
                getEngine().onUsers(msg.getUsers());
                getEngine().getGroupsEngine().onGroupsUpdated(msg.getChats());
                getEngine().onUpdatedMessage(msg.getMessage());
                chatId = ((TLPeerChat) ((TLMessageService) msg.getMessage()).getToId()).getChatId();

                if (imageUri != null) {
                    application.getSyncKernel().getAvatarUploader().uploadGroup(chatId, new FileUriSource(imageUri.toString()));
                } else if (imageFileName != null) {
                    application.getSyncKernel().getAvatarUploader().uploadGroup(chatId, new FileSource(imageFileName));
                }
            }

            @Override
            public void afterExecute() {
                hideKeyboard(chatTitleView);
                getRootController().onChatCreated(chatId);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard(chatTitleView);
        chatTitleView = null;
        avatarImage = null;
    }
}
