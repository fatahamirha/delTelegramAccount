package org.telegram.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.TelegramApplication;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.fragments.*;
import org.telegram.android.fragments.interfaces.RootController;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLUserContact;
import org.telegram.api.TLUserProfilePhotoEmpty;
import org.telegram.api.TLUserStatusEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 03.09.13 17:17
 */
public class FragmentScreenController implements RootController {
    private StartActivity activity;
    private TelegramApplication application;
    private View container;

    private ArrayList<TelegramFragment> backStack = new ArrayList<TelegramFragment>();

    private Runnable removeBg = new Runnable() {
        @Override
        public void run() {
            container.setBackgroundColor(Color.TRANSPARENT);
        }
    };

    private Runnable addBg = new Runnable() {
        @Override
        public void run() {
            container.setBackgroundColor(Color.WHITE);
            handler.removeCallbacks(removeBg);
            handler.postDelayed(removeBg, 400);
        }
    };

    private Handler handler = new Handler(Looper.getMainLooper());

    public FragmentScreenController(StartActivity activity, Bundle savedState) {
        this.activity = activity;
        this.application = (TelegramApplication) activity.getApplicationContext();
        container = activity.findViewById(R.id.fragmentContainer);

        if (savedState != null && savedState.containsKey("backstack")) {
            String[] keys = savedState.getStringArray("backstack");
            for (int i = 0; i < keys.length; i++) {
                TelegramFragment fragment = (TelegramFragment) activity.getSupportFragmentManager().findFragmentByTag(keys[i]);
                if (fragment != null) {
                    backStack.add(fragment);
                }
            }
            if (backStack.size() > 0) {
                backStack.get(backStack.size() - 1).setHasOptionsMenu(true);
            }
        }
    }

    private void onStartTransaction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            handler.post(addBg);
        }
    }

    public Bundle saveState() {
        Bundle bundle = new Bundle();
        String[] keys = new String[backStack.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = backStack.get(i).getTag();
        }
        bundle.putStringArray("backstack", keys);
        return bundle;
    }

    private FragmentTransaction prepareTransaction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit);
        } else {
            return activity.getSupportFragmentManager().beginTransaction();
        }
    }

    private FragmentTransaction prepareBackTransaction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_close_enter, R.anim.fragment_close_exit);
        } else {
            return activity.getSupportFragmentManager().beginTransaction();
        }
//        if (application.getScreenLogicType() == ScreenLogicType.SINGLE_ANIMATED) {
//            application.getResponsibility().doPause(300);
//            application.getImageController().doPause(300);
//            return activity.getSupportFragmentManager().beginTransaction()
//                    .setCustomAnimations(R.anim.fragment_close_enter, R.anim.fragment_close_exit);
//        } else {
//            return activity.getSupportFragmentManager().beginTransaction();
//        }
//        return activity.getSupportFragmentManager().beginTransaction()
//                .setCustomAnimations(R.anim.fragment_close_enter, R.anim.fragment_close_exit);

    }

    public void doUp() {
        if (backStack.size() > 1) {
            TelegramFragment currentFragment = backStack.get(backStack.size() - 1);

            int count = 1;
            for (int i = backStack.size() - 2; i > 0; i--) {
                if (!currentFragment.isParentFragment(backStack.get(i))) {
                    count++;
                } else {
                    break;
                }
            }

            popFragment(count);

            // TelegramFragment rootFragment = backStack.get(0);
            // rootFragment.setHasOptionsMenu(true);

//            prepareBackTransaction()
//                    .remove(backStack.get(backStack.size() - 1))
//                    .attach(rootFragment).commit();

//            while (backStack.size() > 1) {
//                backStack.remove(backStack.size() - 1);
//            }
        }
    }

    public boolean doSystemBack() {
        if (backStack.size() > 1) {
            TelegramFragment currentFragment = backStack.get(backStack.size() - 1);
            if (!currentFragment.onBackPressed()) {
                TelegramFragment prevFragment = backStack.get(backStack.size() - 2);
                prevFragment.setHasOptionsMenu(true);
                backStack.remove(backStack.size() - 1);
                prepareBackTransaction()
                        .remove(currentFragment)
                        .attach(prevFragment)
                        .commit();
                onStartTransaction();
            }
            return true;
        } else {
            return false;
        }
    }

    private void openScreen(TelegramFragment fragment) {
        openScreen(fragment, false);
    }

    private void openScreen(TelegramFragment fragment, boolean forceNoAnimation) {
        fragment.setHasOptionsMenu(true);
        FragmentTransaction transaction;
        if (!forceNoAnimation) {
            transaction = prepareTransaction();
        } else {
            transaction = activity.getSupportFragmentManager().beginTransaction();
        }
        if (backStack.size() > 0) {
            TelegramFragment backFragment = backStack.get(backStack.size() - 1);
            if (backFragment.isSaveInStack()) {
                transaction.detach(backFragment);
            } else {
                transaction.remove(backFragment);
                backStack.remove(backStack.size() - 1);
            }
            transaction.add(R.id.fragmentContainer, fragment, "backstack#" + backStack.size());
        } else {
            transaction.replace(R.id.fragmentContainer, fragment, "backstack#" + backStack.size());
        }

        transaction.commit();

        onStartTransaction();

        backStack.add(fragment);
    }

    @Override
    public void popFragment(int count) {
        for (int i = 0; i < count - 1; i++) {
            backStack.remove(backStack.size() - 2);
        }

        if (backStack.size() > 1) {
            TelegramFragment currentFragment = backStack.get(backStack.size() - 1);
            TelegramFragment prevFragment = backStack.get(backStack.size() - 2);
            prevFragment.setHasOptionsMenu(true);
            backStack.remove(backStack.size() - 1);
            prepareBackTransaction()
                    .remove(currentFragment)
                    .attach(prevFragment)
                    .commit();

            onStartTransaction();
        }
    }

    @Override
    public void shareContact(int uid) {
        openScreen(DialogsFragment.buildSendContact(uid));
    }

    @Override
    public void forwardMessage(int mid) {
        openScreen(DialogsFragment.buildForwardMessage(mid));
    }

    @Override
    public void forwardMessages(Integer[] mid) {
        openScreen(DialogsFragment.buildForwardMessages(mid));
    }

    @Override
    public void sendText(String text) {
        openScreen(DialogsFragment.buildSendMessage(text));
        // backStack.remove(backStack.size() - 2);
    }

    @Override
    public void sendImage(String uri) {
        openScreen(DialogsFragment.buildSendImage(uri));
        // backStack.remove(backStack.size() - 2);
    }

    @Override
    public void sendImages(String[] uris) {
        openScreen(DialogsFragment.buildSendImages(uris));
        // backStack.remove(backStack.size() - 2);
    }

    @Override
    public void sendVideo(String uri) {
        openScreen(DialogsFragment.buildSendVideo(uri));
        // backStack.remove(backStack.size() - 2);
    }

    @Override
    public void sendDoc(String uri) {
        openScreen(DialogsFragment.buildSendDoc(uri));
    }

    @Override
    public void sendDocs(String[] uri) {
        openScreen(DialogsFragment.buildSendDocs(uri));
    }

    @Override
    public void doBack() {
        doSystemBack();
    }

    @Override
    public void openDialogs(boolean fromRoot) {
        openScreen(new DialogsFragment(), fromRoot);
    }

    @Override
    public void showPanel() {
        activity.showBar();
    }

    @Override
    public void hidePanel() {
        activity.hideBar();
    }

    @Override
    public void openApp() {
        activity.openApp();
    }

    @Override
    public void openDialog(int peerType, int peerId) {
        if (peerType == PeerType.PEER_CHAT) {
            DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
            if (description == null) {
                return;
            }
        } else if (peerType == PeerType.PEER_USER) {
            User user = application.getEngine().getUser(peerId);
            if (user == null) {
                if (peerId != 333000) {
                    return;
                } else {
                    List<TLAbsUser> users = new ArrayList<TLAbsUser>();
                    users.add(new TLUserContact(333000, "Telegram", "", 0, "333", new TLUserProfilePhotoEmpty(), new TLUserStatusEmpty()));
                    application.getEngine().onUsers(users);
                }
            }
        }

        int index = -1;
        for (int i = 0; i < backStack.size(); i++) {
            Fragment fragment = backStack.get(i);
            if (fragment instanceof ConversationFragment) {
                ConversationFragment conversationFragment = (ConversationFragment) fragment;
                if (conversationFragment.getPeerId() == peerId && conversationFragment.getPeerType() == peerType) {
                    index = i;
                    break;
                }
            }
        }

        openScreen(new ConversationFragment(peerType, peerId), false);

        if (index >= 0) {
            backStack.remove(index);
        }
    }

    @Override
    public void openMedia(int peerType, int peerId) {
        openScreen(new MediaFragment(peerType, peerId));
    }

    @Override
    public void openImageAnimated(int mid, int peerType, int peerId, View view, Bitmap preview, int x, int y) {
        activity.openImageAnimated(mid, peerType, peerId, view, preview, x, y);
    }

    @Override
    public void openImage(int mid, int peerType, int peerId) {
        activity.openImage(mid, peerType, peerId);
    }

    @Override
    public void openAvatarPreview(TLLocalFileLocation fileLocation) {
        activity.openAvatarPreview(fileLocation);
    }

    @Override
    public void openUser(int uid) {
        if (uid == application.getCurrentUid()) {
            openSettings();
        } else {

            int index = -1;
            for (int i = 0; i < backStack.size(); i++) {
                Fragment fragment = backStack.get(i);
                if (fragment instanceof ProfileFragment) {
                    ProfileFragment profileFragment = (ProfileFragment) fragment;
                    if (profileFragment.getUserId() == uid) {
                        index = i;
                        break;
                    }
                }
            }

            openScreen(new ProfileFragment(uid));

            if (index >= 0) {
                backStack.remove(index);
            }
        }
    }

    @Override
    public void openSecretChatInfo(int chatId) {
        openScreen(new EncryptedChatInfoFragment(chatId));
    }

    @Override
    public void openKeyPreview(byte[] hash, int uid) {
        openScreen(new KeyPreviewFragment(uid, hash));
    }

    @Override
    public void openContacts() {
        openScreen(new ContactsFragment());
    }

    @Override
    public void openSettings() {
        openScreen(new SettingsFragment());
        // activity.startActivity(new Intent().setClass(activity, SettingsActivity.class));
    }

    @Override
    public void openNameSettings() {
        openScreen(new SettingsNameFragment());
    }

    @Override
    public void openDebugSettings() {
        openScreen(new DebugFragment());
    }

    @Override
    public void openNotificationSettings() {
        openScreen(new SettingsNottificationsFragment());
    }

    @Override
    public void openWallpaperSettings() {
        openScreen(new WallpapersFragment());
    }

    @Override
    public void openChatSettings() {
        openScreen(new SettingsChatFragment());
    }

    @Override
    public void openBlocked() {
        openScreen(new BlockedFragment());
    }

    @Override
    public void onLogout() {
        FragmentTransaction transaction = prepareTransaction();
        for (TelegramFragment fragment : backStack) {
            transaction.remove(fragment);
        }
        StartActivity.isGuideShown = false;
        transaction.add(R.id.fragmentContainer, new TourFragment(), "tourFragment")
                .commit();
        onStartTransaction();
        hidePanel();
        backStack.clear();
    }

    @Override
    public void pickUser() {
        openScreen(new PickUserFragment());
    }

    @Override
    public void pickFile() {
        openScreen(new PickFileFragment());
    }

    @Override
    public void pickLocations() {
        openScreen(new PickLocationFragment());
    }

    @Override
    public void viewLocation(double lat, double lon, int uid) {
        openScreen(new ViewLocationFragment(uid, lat, lon));
    }

    @Override
    public void openCreateNewChat() {
        openScreen(new CreateChatFragment());
    }

    @Override
    public void completeCreateChat(int[] uids) {
        openScreen(new CreateChatCompleteFragment(uids));
        backStack.remove(backStack.size() - 2);
    }

    @Override
    public void onChatCreated(int chatId) {
        openDialog(PeerType.PEER_CHAT, chatId);
        backStack.remove(backStack.size() - 2);
    }

    @Override
    public void openChatEdit(int chatId) {
        openScreen(new EditChatFragment(chatId));
    }

    @Override
    public void openChatEditTitle(int chatId) {
        openScreen(new EditChatTitleFragment(chatId));
    }

    @Override
    public void addContact(int uid) {
        User user = application.getEngine().getUser(uid);
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, user.getDisplayName());
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, "+" + user.getPhone());

        intent.putExtra("finishActivityOnSaveCompleted", true);

        activity.startActivity(intent);

        application.getSyncKernel().getContactsSync().addPhoneMapping(uid, user.getPhone());
    }

    @Override
    public void editContactName(int uid) {
        // openScreen(new EditContactNameFragment(uid));
    }
}