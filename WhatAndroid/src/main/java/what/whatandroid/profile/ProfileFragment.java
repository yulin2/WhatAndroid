package what.whatandroid.profile;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Date;

import api.cli.Utils;
import api.index.Index;
import api.soup.MySoup;
import api.user.Profile;
import api.user.User;
import api.user.UserProfile;
import api.user.recent.UserRecents;
import what.whatandroid.R;
import what.whatandroid.callbacks.LoadingListener;
import what.whatandroid.callbacks.OnLoggedInCallback;
import what.whatandroid.callbacks.SetTitleCallback;
import what.whatandroid.forums.thread.ReplyDialogFragment;
import what.whatandroid.imgloader.ImageLoadingListener;
import what.whatandroid.settings.SettingsActivity;

/**
 * Fragment to display a user's profile
 */
public class ProfileFragment extends Fragment implements OnLoggedInCallback,
	LoaderManager.LoaderCallbacks<UserProfile>, ReplyDialogFragment.ReplyDialogListener {

	public static final String SEND_MSG_FILTER = "ProfileFragment_receiver";
	public static final String DEFER_LOADING = "what.whatandroid.DEFER_LOADING";
	/**
	 * The user's profile information
	 */
	private UserProfile userProfile;
	/**
	 * The user id we want to view, passed earlier as a param since we defer loading until onCreate
	 */
	private int userID;
	private boolean deferLoad;
	/**
	 * Callbacks to the activity so we can go set the title
	 */
	private SetTitleCallback setTitle;
	private LoadingListener<Index> indexLoadingListener;
	/**
	 * Various content views displaying the user's information
	 */
	private ImageView avatar;
	private ProgressBar spinner;
	private View artContainer;
	/**
	 * The user's stats being shown
	 */
	private TextView username, userClass, joined, upload, download, ratio, paranoia;
	/**
	 * Text views saying what the various numbers in the profile mean, so we can hide those that are hidden
	 * by the user's paranoia
	 */
	private TextView uploadText, downloadText, ratioText, paranoiaText;
	/**
	 * View pagers & adapters for displaying the lists of recent snatches and uploads & headers for the views
	 * headers are needed so we can hide the views if hidden by paranoia
	 */
	private RecentTorrentPagerAdapter snatchesAdapter, uploadsAdapter;
	private View snatchesContainer, uploadsContainer, donor, warned, banned;
	/**
	 * Draft of a message we're writing to the user
	 */
	private String messageDraft = "", messageSubject = "";
	/**
	 * Send message menu item, so we can hide it if we're viewing our own profile
	 */
	private MenuItem sendMessage;
	private SendMessageReceiver receiver;

	/**
	 * Use this factory method to create a new instance of the fragment displaying the
	 * desired user's profile
	 *
	 * @param id        The user id to display the profile of
	 * @param deferLoad True if the fragment should wait to load the profile until the user id is updated
	 */
	public static ProfileFragment newInstance(int id, boolean deferLoad){
		ProfileFragment fragment = new ProfileFragment();
		Bundle args = new Bundle();
		args.putInt(ProfileActivity.USER_ID, id);
		args.putBoolean(ProfileFragment.DEFER_LOADING, deferLoad);
		fragment.setArguments(args);
		return fragment;
	}

	public ProfileFragment(){
		// Required empty public constructor
	}

	public void setUserID(int id){
		if (deferLoad){
			userID = id;
			getArguments().putInt(ProfileActivity.USER_ID, userID);
			//We now have the right id so we don't need to defer loading anymore
			getArguments().putBoolean(ProfileFragment.DEFER_LOADING, false);
			Bundle args = new Bundle();
			args.putInt(ProfileActivity.USER_ID, userID);
			getLoaderManager().initLoader(0, args, this);
		}
	}

	/**
	 * Get the user id the fragment is currently viewing
	 *
	 * @return viewed user's id
	 */
	public int getUserID(){
		return userID;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		try {
			setTitle = (SetTitleCallback)activity;
			indexLoadingListener = (LoadingListener<Index>)activity;
		}
		catch (ClassCastException e){
			throw new ClassCastException(activity.toString() + " must implement ViewTorrent & SetTitle Callbacks");
		}
		receiver = new SendMessageReceiver();
		LocalBroadcastManager.getInstance(getActivity())
			.registerReceiver(receiver, new IntentFilter(SEND_MSG_FILTER));
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (receiver != null) {
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		userID = getArguments().getInt(ProfileActivity.USER_ID);
		deferLoad = getArguments().getBoolean(ProfileFragment.DEFER_LOADING);
		setHasOptionsMenu(true);
		if (savedInstanceState != null){
			messageDraft = savedInstanceState.getString(ReplyDialogFragment.DRAFT);
			messageSubject = savedInstanceState.getString(ReplyDialogFragment.SUBJECT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_profile, container, false);
		avatar = (ImageView)view.findViewById(R.id.avatar);
		spinner = (ProgressBar)view.findViewById(R.id.loading_indicator);
		artContainer = view.findViewById(R.id.art_container);
		username = (TextView)view.findViewById(R.id.username);
		userClass = (TextView)view.findViewById(R.id.user_class);
		joined = (TextView)view.findViewById(R.id.joined);
		upload = (TextView)view.findViewById(R.id.upload);
		uploadText = (TextView)view.findViewById(R.id.uploaded_text);
		download = (TextView)view.findViewById(R.id.download);
		downloadText = (TextView)view.findViewById(R.id.downloaded_text);
		ratio = (TextView)view.findViewById(R.id.ratio);
		ratioText = (TextView)view.findViewById(R.id.ratio_text);
		paranoia = (TextView)view.findViewById(R.id.paranoia);
		paranoiaText = (TextView)view.findViewById(R.id.paranoia_text);
		//Hide the paranoia text until we figure out what the user's paranoia settings are
		paranoiaText.setVisibility(View.GONE);
		ViewPager recentSnatches = (ViewPager)view.findViewById(R.id.recent_snatches);
		snatchesContainer = view.findViewById(R.id.snatches_container);
		ViewPager recentUploads = (ViewPager)view.findViewById(R.id.recent_uploads);
		uploadsContainer = view.findViewById(R.id.uploads_container);
		donor = view.findViewById(R.id.donor);
		warned = view.findViewById(R.id.warned);
		banned = view.findViewById(R.id.banned);

		snatchesAdapter = new RecentTorrentPagerAdapter(getChildFragmentManager());
		uploadsAdapter = new RecentTorrentPagerAdapter(getChildFragmentManager());
		recentSnatches.setAdapter(snatchesAdapter);
		recentUploads.setAdapter(uploadsAdapter);

		if (MySoup.isLoggedIn() && !deferLoad){
			//We could get -1 user id if we were logged out and trying to view our own profile, so update it
			if (userID == -1){
				userID = MySoup.getUserId();
				getArguments().putInt(ProfileActivity.USER_ID, userID);
			}
			Bundle args = new Bundle();
			args.putInt(ProfileActivity.USER_ID, userID);
			getLoaderManager().initLoader(0, args, this);
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putString(ReplyDialogFragment.DRAFT, messageDraft);
		outState.putString(ReplyDialogFragment.SUBJECT, messageSubject);
	}

	@Override
	public void onLoggedIn(){
		if (isAdded() && !deferLoad){
			//We could get -1 user id if we were logged out and trying to view our own profile, so update it
			if (userID == -1){
				userID = MySoup.getUserId();
				getArguments().putInt(ProfileActivity.USER_ID, userID);
			}
			Bundle args = new Bundle();
			args.putInt(ProfileActivity.USER_ID, userID);
			getLoaderManager().initLoader(0, args, this);
		}
	}

	@Override
	public Loader<UserProfile> onCreateLoader(int id, Bundle args){
		if (isAdded()){
			getActivity().setProgressBarIndeterminate(true);
			getActivity().setProgressBarIndeterminateVisibility(true);
		}
		return new ProfileAsyncLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<UserProfile> loader, UserProfile data){
		getActivity().setProgressBarIndeterminate(false);
		getActivity().setProgressBarIndeterminateVisibility(false);
		if (data == null || !data.getStatus()){
			Toast.makeText(getActivity(), "Could not load profile", Toast.LENGTH_LONG).show();
		}
		else {
			userProfile = data;
			populateViews();
			if (indexLoadingListener != null){
				indexLoadingListener.onLoadingComplete(MySoup.getIndex());
			}
			if (userID != MySoup.getUserId() && sendMessage != null){
				sendMessage.setVisible(true);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<UserProfile> loader){
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.profile, menu);
		sendMessage = menu.findItem(R.id.action_message);
		if (userProfile != null && userID != MySoup.getUserId()){
			sendMessage.setVisible(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if (item.getItemId() == R.id.action_refresh){
			Bundle args = new Bundle();
			args.putInt(ProfileActivity.USER_ID, userID);
			getLoaderManager().restartLoader(0, args, this);
		}
		if (item.getItemId() == R.id.action_message){
			showReplyDialog();
		}
		return false;
	}

	@Override
	public void post(String message, String subject){
		messageDraft = "";
		messageSubject = "";
		Intent sendMsg = new Intent(getActivity(), SendMessageService.class);
		sendMsg.putExtra("userID", userID);
		String[] params = { subject, message };
		sendMsg.putExtra("params", params);
		getActivity().startService(sendMsg);
	}

	@Override
	public void saveDraft(String message, String subject){
		messageDraft = message;
		messageSubject = subject;
	}

	@Override
	public void discard(){
		messageDraft = "";
		messageSubject = "";
	}

	/**
	 * Display the compose reply dialog so the user can write their response
	 */
	private void showReplyDialog(){
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null){
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ReplyDialogFragment reply = ReplyDialogFragment.newInstance(messageDraft, messageSubject);
		reply.setTargetFragment(this, 0);
		reply.show(ft, "dialog");
	}

	/**
	 * Update the profile fields with the information we loaded. We need to do a ton of checks here to
	 * properly handle user's various paranoia configurations, which could cause us to get a null for any of the
	 * fields that can be hidden. We also hide the recent snatches/uploads if the user's paranoia is high (6+).
	 * When viewing our own profile we'll get all the data back but will still see our paranoia value so we need to
	 * ignore the paranoia if it's our own profile
	 */
	private void populateViews(){
		Profile profile = userProfile.getUser().getProfile();
		setTitle.setTitle(profile.getUsername());
		username.setText(profile.getUsername());
		userClass.setText(profile.getPersonal().getUserClass());
		Date joinDate = MySoup.parseDate(profile.getStats().getJoinedDate());
		joined.setText("Joined " + DateUtils.getRelativeTimeSpanString(joinDate.getTime(),
			new Date().getTime(), DateUtils.WEEK_IN_MILLIS));

		//We need to check all the paranoia cases that may cause a field to be missing and hide the views for it
		String avatarUrl = profile.getAvatar();
		if (SettingsActivity.imagesEnabled(getActivity()) && avatarUrl != null && !avatarUrl.isEmpty()){
			ImageLoader.getInstance().displayImage(profile.getAvatar(), avatar,
				new ImageLoadingListener(spinner, artContainer));
		}
		else {
			artContainer.setVisibility(View.GONE);
		}
		if (profile.getPersonal().getParanoia().intValue() > 0 && userID != MySoup.getUserId()){
			paranoiaText.setVisibility(View.VISIBLE);
			paranoia.setText(profile.getPersonal().getParanoiaText());
		}
		else {
			paranoia.setVisibility(View.GONE);
		}
		if (profile.getStats().getUploaded() != null){
			upload.setText(Utils.toHumanReadableSize(profile.getStats().getUploaded().longValue()));
		}
		else {
			uploadText.setVisibility(View.GONE);
			upload.setVisibility(View.GONE);
		}
		if (profile.getStats().getDownloaded() != null){
			download.setText(Utils.toHumanReadableSize(profile.getStats().getDownloaded().longValue()));
		}
		else {
			downloadText.setVisibility(View.GONE);
			download.setVisibility(View.GONE);
		}
		if (profile.getStats().getRatio() != null && profile.getStats().getRequiredRatio() != null){
			ratio.setText(String.format("%.2f", profile.getStats().getRatio().floatValue())
				+ " / " + String.format("%.2f", profile.getStats().getRequiredRatio().floatValue()));
		}
		else {
			ratioText.setVisibility(View.GONE);
			ratio.setVisibility(View.GONE);
		}
		//TODO: Keep an eye on this API endpoint and watch for when it starts respecting paranoia and we get null back
		UserRecents recentTorrents = userProfile.getUserRecents();
		if (profile.getPersonal().getParanoia().intValue() < 6 || userID == MySoup.getUserId()){
			if (recentTorrents.getSnatches().size() > 0){
				snatchesAdapter.onLoadingComplete(recentTorrents.getSnatches());
				snatchesAdapter.notifyDataSetChanged();
			}
			else {
				snatchesContainer.setVisibility(View.GONE);
			}
			if (recentTorrents.getUploads().size() > 0){
				uploadsAdapter.onLoadingComplete(recentTorrents.getUploads());
				uploadsAdapter.notifyDataSetChanged();
			}
			else {
				uploadsContainer.setVisibility(View.GONE);
			}
		}
		else {
			snatchesContainer.setVisibility(View.GONE);
			uploadsContainer.setVisibility(View.GONE);
		}

		if (!profile.getPersonal().isDonor()){
			donor.setVisibility(View.GONE);
		}
		if (!profile.getPersonal().isWarned()){
			warned.setVisibility(View.GONE);
		}
		if (profile.getPersonal().isEnabled()){
			banned.setVisibility(View.GONE);
		}
	}

	private class SendMessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context receiverContext, Intent receiverIntent){
			Boolean status = receiverIntent.getBooleanExtra("status", false);
			if (!status){
				Toast.makeText(getActivity(), "Could not send message", Toast.LENGTH_LONG).show();
		}
			else {
				Toast.makeText(getActivity(), "Message sent", Toast.LENGTH_SHORT).show();
			}
	}
	}

	public static class SendMessageService extends IntentService {
		private int userID;

		public SendMessageService() {
			super("SendMessageService");
		}

		public void onHandleIntent(Intent intent) {
			userID = intent.getIntExtra("userID", 0);
			String[] params = intent.getStringArrayExtra("params");
			Intent resultIntent = new Intent(SEND_MSG_FILTER);
			resultIntent.putExtra("status", User.sendMessage(userID, params[0], params[1]));
			LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
			return;
		}
	}
}
