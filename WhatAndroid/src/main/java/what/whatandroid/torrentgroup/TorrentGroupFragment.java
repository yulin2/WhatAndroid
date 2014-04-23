package what.whatandroid.torrentgroup;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.Toast;
import api.torrents.torrents.TorrentGroup;
import com.astuetz.PagerSlidingTabStrip;
import what.whatandroid.R;
import what.whatandroid.callbacks.LoadingListener;

/**
 * Fragment for showing swipeable views of the torrent group overview and comments
 */
public class TorrentGroupFragment extends android.support.v4.app.Fragment implements LoadingListener<TorrentGroup> {
	private TorrentGroupPagerAdapter torrentGroupPagerAdapter;
	private TorrentGroup group;
	/**
	 * Track the bookmark menu item so we can change the icon depending on the group's bookmark
	 * status and hide it if we haven't loaded yet
	 */
	private MenuItem bookmarkMenu;

	public static TorrentGroupFragment newInstance(int groupId){
		TorrentGroupFragment f = new TorrentGroupFragment();
		Bundle args = new Bundle();
		args.putInt(TorrentGroupActivity.GROUP_ID, groupId);
		f.setArguments(args);
		return f;
	}

	public TorrentGroupFragment(){
		//Required empty ctor
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		int groupId = getArguments().getInt(TorrentGroupActivity.GROUP_ID);
		View view = inflater.inflate(R.layout.fragment_view_pager_tabs, container, false);
		ViewPager viewPager = (ViewPager)view.findViewById(R.id.pager);
		torrentGroupPagerAdapter = new TorrentGroupPagerAdapter(getChildFragmentManager(), groupId);
		viewPager.setAdapter(torrentGroupPagerAdapter);
		PagerSlidingTabStrip tabs = (PagerSlidingTabStrip)view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.torrent_group, menu);
		bookmarkMenu = menu.findItem(R.id.action_bookmark);
		if (group != null){
			if (group.getResponse().getGroup().isBookmarked()){
				bookmarkMenu.setIcon(R.drawable.ic_bookmark_on);
			}
			else {
				bookmarkMenu.setIcon(R.drawable.ic_bookmark_off);
			}
		}
		else {
			bookmarkMenu.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if (item.getItemId() == R.id.action_bookmark){
			new ToggleBookmarkTask().execute(group);
			return true;
		}
		return false;
	}

	@Override
	public void onLoadingComplete(TorrentGroup data){
		group = data;
		torrentGroupPagerAdapter.onLoadingComplete(group);
		if (bookmarkMenu != null){
			bookmarkMenu.setVisible(true);
			if (group.getResponse().getGroup().isBookmarked()){
				bookmarkMenu.setIcon(R.drawable.ic_bookmark_on);
			}
			else {
				bookmarkMenu.setIcon(R.drawable.ic_bookmark_off);
			}
		}
	}

	private class ToggleBookmarkTask extends AsyncTask<TorrentGroup, Void, Boolean> {
		TorrentGroup group;

		@Override
		protected Boolean doInBackground(TorrentGroup... params){
			group = params[0];
			if (group.getResponse().getGroup().isBookmarked()){
				return group.removeBookmark();
			}
			return group.addBookmark();
		}

		@Override
		protected void onPreExecute(){
			bookmarkMenu.setVisible(false);
			if (isAdded()){
				getActivity().setProgressBarIndeterminate(true);
				getActivity().setProgressBarIndeterminateVisibility(true);
			}
		}

		@Override
		protected void onPostExecute(Boolean status){
			if (isAdded()){
				getActivity().setProgressBarIndeterminate(false);
				getActivity().setProgressBarIndeterminateVisibility(false);
			}
			if (!status){
				if (group.getResponse().getGroup().isBookmarked()){
					Toast.makeText(getActivity(), "Could not remove bookmark", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(getActivity(), "Could not add bookmark", Toast.LENGTH_LONG).show();
				}
			}
			else if (bookmarkMenu != null){
				bookmarkMenu.setVisible(true);
				//If the group is bookmarked then we removed it, if it's not bookmarked then we added it
				if (group.getResponse().getGroup().isBookmarked()){
					bookmarkMenu.setIcon(R.drawable.ic_bookmark_on);
				}
				else {
					bookmarkMenu.setIcon(R.drawable.ic_bookmark_off);
				}
			}
		}
	}
}
