/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package com.porogram.profosure1.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.porogram.profosure1.messenger.AndroidUtilities;
import com.porogram.profosure1.messenger.ApplicationLoader;
import com.porogram.profosure1.messenger.ContactsController;
import com.porogram.profosure1.messenger.LocaleController;
import com.porogram.profosure1.messenger.MessagesController;
import com.porogram.profosure1.messenger.NotificationCenter;
import com.porogram.profosure1.messenger.FileLog;
import com.porogram.profosure1.messenger.R;
import com.porogram.profosure1.messenger.support.widget.LinearLayoutManager;
import com.porogram.profosure1.messenger.support.widget.RecyclerView;
import com.porogram.profosure1.tgnet.ConnectionsManager;
import com.porogram.profosure1.tgnet.RequestDelegate;
import com.porogram.profosure1.tgnet.TLObject;
import com.porogram.profosure1.tgnet.TLRPC;
import com.porogram.profosure1.messenger.UserConfig;
import com.porogram.profosure1.ui.ActionBar.ActionBar;
import com.porogram.profosure1.ui.ActionBar.AlertDialog;
import com.porogram.profosure1.ui.ActionBar.BaseFragment;
import com.porogram.profosure1.ui.ActionBar.Theme;
import com.porogram.profosure1.ui.ActionBar.ThemeDescription;
import com.porogram.profosure1.ui.Cells.HeaderCell;
import com.porogram.profosure1.ui.Cells.TextCheckCell;
import com.porogram.profosure1.ui.Cells.TextInfoPrivacyCell;
import com.porogram.profosure1.ui.Cells.TextSettingsCell;
import com.porogram.profosure1.ui.Components.LayoutHelper;
import com.porogram.profosure1.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class PrivacySettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int privacySectionRow;
    private int blockedRow;
    private int lastSeenRow;
    private int callsRow;
    private int groupsRow;
    private int groupsDetailRow;
    private int securitySectionRow;
    private int sessionsRow;
    private int passwordRow;
    private int passcodeRow;
    private int sessionsDetailRow;
    private int deleteAccountSectionRow;
    private int deleteAccountRow;
    private int deleteAccountDetailRow;
    private int secretSectionRow;
    private int secretWebpageRow;
    private int secretDetailRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        ContactsController.getInstance().loadPrivacySettings();

        rowCount = 0;
        privacySectionRow = rowCount++;
        blockedRow = rowCount++;
        lastSeenRow = rowCount++;
        if (MessagesController.getInstance().callsEnabled) {
            callsRow = rowCount++;
        } else {
            callsRow = -1;
        }
        groupsRow = rowCount++;
        groupsDetailRow = rowCount++;
        securitySectionRow = rowCount++;
        passcodeRow = rowCount++;
        passwordRow = rowCount++;
        sessionsRow = rowCount++;
        sessionsDetailRow = rowCount++;
        deleteAccountSectionRow = rowCount++;
        deleteAccountRow = rowCount++;
        deleteAccountDetailRow = rowCount++;
        if (MessagesController.getInstance().secretWebpagePreview != 1) {
            secretSectionRow = rowCount++;
            secretWebpageRow = rowCount++;
            secretDetailRow = rowCount++;
        } else {
            secretSectionRow = -1;
            secretWebpageRow = -1;
            secretDetailRow = -1;
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.privacyRulesUpdated);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.privacyRulesUpdated);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("PrivacySettings", R.string.PrivacySettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (!view.isEnabled()) {
                    return;
                }
                if (position == blockedRow) {
                    presentFragment(new BlockedUsersActivity());
                } else if (position == sessionsRow) {
                    presentFragment(new SessionsActivity());
                } else if (position == deleteAccountRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("DeleteAccountTitle", R.string.DeleteAccountTitle));
                    builder.setItems(new CharSequence[]{
                            LocaleController.formatPluralString("Months", 1),
                            LocaleController.formatPluralString("Months", 3),
                            LocaleController.formatPluralString("Months", 6),
                            LocaleController.formatPluralString("Years", 1)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int value = 0;
                            if (which == 0) {
                                value = 30;
                            } else if (which == 1) {
                                value = 90;
                            } else if (which == 2) {
                                value = 182;
                            } else if (which == 3) {
                                value = 365;
                            }
                            final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 1);
                            progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                            progressDialog.show();

                            final TLRPC.TL_account_setAccountTTL req = new TLRPC.TL_account_setAccountTTL();
                            req.ttl = new TLRPC.TL_accountDaysTTL();
                            req.ttl.days = value;
                            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                                @Override
                                public void run(final TLObject response, final TLRPC.TL_error error) {
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e) {
                                                FileLog.e(e);
                                            }
                                            if (response instanceof TLRPC.TL_boolTrue) {
                                                ContactsController.getInstance().setDeleteAccountTTL(req.ttl.days);
                                                listAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (position == lastSeenRow) {
                    presentFragment(new PrivacyControlActivity(0));
                } else if (position == callsRow) {
                    presentFragment(new PrivacyControlActivity(2));
                } else if (position == groupsRow) {
                    presentFragment(new PrivacyControlActivity(1));
                } else if (position == passwordRow) {
                    presentFragment(new TwoStepVerificationActivity(0));
                } else if (position == passcodeRow) {
                    if (UserConfig.passcodeHash.length() > 0) {
                        presentFragment(new PasscodeActivity(2));
                    } else {
                        presentFragment(new PasscodeActivity(0));
                    }
                } else if (position == secretWebpageRow) {
                    if (MessagesController.getInstance().secretWebpagePreview == 1) {
                        MessagesController.getInstance().secretWebpagePreview = 0;
                    } else {
                        MessagesController.getInstance().secretWebpagePreview = 1;
                    }
                    ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().putInt("secretWebpage2", MessagesController.getInstance().secretWebpagePreview).commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MessagesController.getInstance().secretWebpagePreview == 1);
                    }
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.privacyRulesUpdated) {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    private String formatRulesString(int rulesType) {
        ArrayList<TLRPC.PrivacyRule> privacyRules = ContactsController.getInstance().getPrivacyRules(rulesType);
        if (privacyRules.size() == 0) {
            return LocaleController.getString("LastSeenNobody", R.string.LastSeenNobody);
        }
        int type = -1;
        int plus = 0;
        int minus = 0;
        for (int a = 0; a < privacyRules.size(); a++) {
            TLRPC.PrivacyRule rule = privacyRules.get(a);
            if (rule instanceof TLRPC.TL_privacyValueAllowUsers) {
                plus += rule.users.size();
            } else if (rule instanceof TLRPC.TL_privacyValueDisallowUsers) {
                minus += rule.users.size();
            } else if (rule instanceof TLRPC.TL_privacyValueAllowAll) {
                type = 0;
            } else if (rule instanceof TLRPC.TL_privacyValueDisallowAll) {
                type = 1;
            } else {
                type = 2;
            }
        }
        if (type == 0 || type == -1 && minus > 0) {
            if (minus == 0) {
                return LocaleController.getString("LastSeenEverybody", R.string.LastSeenEverybody);
            } else {
                return LocaleController.formatString("LastSeenEverybodyMinus", R.string.LastSeenEverybodyMinus, minus);
            }
        } else if (type == 2 || type == -1 && minus > 0 && plus > 0) {
            if (plus == 0 && minus == 0) {
                return LocaleController.getString("LastSeenContacts", R.string.LastSeenContacts);
            } else {
                if (plus != 0 && minus != 0) {
                    return LocaleController.formatString("LastSeenContactsMinusPlus", R.string.LastSeenContactsMinusPlus, minus, plus);
                } else if (minus != 0) {
                    return LocaleController.formatString("LastSeenContactsMinus", R.string.LastSeenContactsMinus, minus);
                } else {
                    return LocaleController.formatString("LastSeenContactsPlus", R.string.LastSeenContactsPlus, plus);
                }
            }
        } else if (type == 1 || plus > 0) {
            if (plus == 0) {
                return LocaleController.getString("LastSeenNobody", R.string.LastSeenNobody);
            } else {
                return LocaleController.formatString("LastSeenNobodyPlus", R.string.LastSeenNobodyPlus, plus);
            }
        }
        return "unknown";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == passcodeRow || position == passwordRow || position == blockedRow || position == sessionsRow || position == secretWebpageRow ||
                    position == groupsRow && !ContactsController.getInstance().getLoadingGroupInfo() ||
                    position == lastSeenRow && !ContactsController.getInstance().getLoadingLastSeenInfo() ||
                    position == callsRow && !ContactsController.getInstance().getLoadingCallsInfo() ||
                    position == deleteAccountRow && !ContactsController.getInstance().getLoadingDeleteInfo();
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 2:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                default:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == blockedRow) {
                        textCell.setText(LocaleController.getString("BlockedUsers", R.string.BlockedUsers), true);
                    } else if (position == sessionsRow) {
                        textCell.setText(LocaleController.getString("SessionsTitle", R.string.SessionsTitle), false);
                    } else if (position == passwordRow) {
                        textCell.setText(LocaleController.getString("TwoStepVerification", R.string.TwoStepVerification), true);
                    } else if (position == passcodeRow) {
                        textCell.setText(LocaleController.getString("Passcode", R.string.Passcode), true);
                    } else if (position == lastSeenRow) {
                        String value;
                        if (ContactsController.getInstance().getLoadingLastSeenInfo()) {
                            value = LocaleController.getString("Loading", R.string.Loading);
                        } else {
                            value = formatRulesString(0);
                        }
                        textCell.setTextAndValue(LocaleController.getString("PrivacyLastSeen", R.string.PrivacyLastSeen), value, true);
                    } else if (position == callsRow) {
                        String value;
                        if (ContactsController.getInstance().getLoadingCallsInfo()) {
                            value = LocaleController.getString("Loading", R.string.Loading);
                        } else {
                            value = formatRulesString(2);
                        }
                        textCell.setTextAndValue(LocaleController.getString("Calls", R.string.Calls), value, true);
                    } else if (position == groupsRow) {
                        String value;
                        if (ContactsController.getInstance().getLoadingGroupInfo()) {
                            value = LocaleController.getString("Loading", R.string.Loading);
                        } else {
                            value = formatRulesString(1);
                        }
                        textCell.setTextAndValue(LocaleController.getString("GroupsAndChannels", R.string.GroupsAndChannels), value, false);
                    } else if (position == deleteAccountRow) {
                        String value;
                        if (ContactsController.getInstance().getLoadingDeleteInfo()) {
                            value = LocaleController.getString("Loading", R.string.Loading);
                        } else {
                            int ttl = ContactsController.getInstance().getDeleteAccountTTL();
                            if (ttl <= 182) {
                                value = LocaleController.formatPluralString("Months", ttl / 30);
                            } else if (ttl == 365) {
                                value = LocaleController.formatPluralString("Years", ttl / 365);
                            } else {
                                value = LocaleController.formatPluralString("Days", ttl);
                            }
                        }
                        textCell.setTextAndValue(LocaleController.getString("DeleteAccountIfAwayFor", R.string.DeleteAccountIfAwayFor), value, false);
                    }
                    break;
                case 1:
                    TextInfoPrivacyCell privacyCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == deleteAccountDetailRow) {
                        privacyCell.setText(LocaleController.getString("DeleteAccountHelp", R.string.DeleteAccountHelp));
                        privacyCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, secretSectionRow == -1 ? R.drawable.greydivider_bottom : R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    } else if (position == groupsDetailRow) {
                        privacyCell.setText(LocaleController.getString("GroupsAndChannelsHelp", R.string.GroupsAndChannelsHelp));
                        privacyCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    } else if (position == sessionsDetailRow) {
                        privacyCell.setText(LocaleController.getString("SessionsInfo", R.string.SessionsInfo));
                        privacyCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    } else if (position == secretDetailRow) {
                        privacyCell.setText("");
                        privacyCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                case 2:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == privacySectionRow) {
                        headerCell.setText(LocaleController.getString("PrivacyTitle", R.string.PrivacyTitle));
                    } else if (position == securitySectionRow) {
                        headerCell.setText(LocaleController.getString("SecurityTitle", R.string.SecurityTitle));
                    } else if (position == deleteAccountSectionRow) {
                        headerCell.setText(LocaleController.getString("DeleteAccountTitle", R.string.DeleteAccountTitle));
                    } else if (position == secretSectionRow) {
                        headerCell.setText(LocaleController.getString("SecretChat", R.string.SecretChat));
                    }
                    break;
                case 3:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    if (position == secretWebpageRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("SecretWebPage", R.string.SecretWebPage), MessagesController.getInstance().secretWebpagePreview == 1, true);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == lastSeenRow || position == blockedRow || position == deleteAccountRow || position == sessionsRow || position == passwordRow || position == passcodeRow || position == groupsRow) {
                return 0;
            } else if (position == deleteAccountDetailRow || position == groupsDetailRow || position == sessionsDetailRow || position == secretDetailRow) {
                return 1;
            } else if (position == securitySectionRow || position == deleteAccountSectionRow || position == privacySectionRow || position == secretSectionRow) {
                return 2;
            } else if (position == secretWebpageRow) {
                return 3;
            }
            return 0;
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        return new ThemeDescription[]{
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, HeaderCell.class, TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite),
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText),

                new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader),

                new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow),
                new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4),

                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumb),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumbChecked),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked),
        };
    }
}