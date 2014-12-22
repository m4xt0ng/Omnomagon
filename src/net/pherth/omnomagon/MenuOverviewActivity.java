package net.pherth.omnomagon;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import net.pherth.omnomagon.data.DataProvider;
import net.pherth.omnomagon.data.Day;
import net.pherth.omnomagon.data.PriceGroup;
import net.pherth.omnomagon.header.FeatureImageHandler;
import net.pherth.omnomagon.header.MensaNameProvider;
import net.pherth.omnomagon.header.MensaNameViewHolder;
import net.pherth.omnomagon.settings.UserPreferences;
import net.pherth.omnomagon.tabs.MenuTabAdapter;
import net.pherth.omnomagon.tabs.MenuTabHandler;
import net.pherth.omnomagon.tabs.WeekdayTabHint;

import java.util.Calendar;
import java.util.List;

//todo landscape layout
public class MenuOverviewActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, DataProvider.DataListener {

    public static final int REQUEST_SETTINGS = 1001;

    private static final long FIVE_MINUTES = 1000L * 60L * 5L;

    private MensaNameProvider _mensaNameProvider;
    private MensaNameViewHolder _mensaNameViewHolder;
    private MenuTabHandler _menuTabHandler;
    private FeatureImageHandler _featureImageHandler;
    private long _lastFeatureImageChange;
    private int _lastPreselectionDay;
    private DataProvider _dataProvider;
    private UserPreferences _userPreferences;

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return _dataProvider;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_overview);
        configureActionBar();
        _mensaNameProvider = new MensaNameProvider(this);
        _mensaNameViewHolder = new MensaNameViewHolder(this);
        _menuTabHandler = new MenuTabHandler(this, this);
        _featureImageHandler = new FeatureImageHandler(this);
        _lastFeatureImageChange = System.currentTimeMillis();
        final Object instance = getLastCustomNonConfigurationInstance();
        if (instance != null && instance instanceof DataProvider) {
            _dataProvider = (DataProvider) instance;
        } else {
            _dataProvider = new DataProvider(this);
        }
        _userPreferences = new UserPreferences(this);
        updateMensaName();
    }

    private void configureActionBar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.support_actionbar);
        setSupportActionBar(toolbar);
        final ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowTitleEnabled(false);
    }

    private void updateMensaName() {
        final String mensaName = _mensaNameProvider.getName();
        _mensaNameViewHolder.setMensaName(mensaName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeFeatureImageAfterFiveMinutes();
        preselectDay();
        configureEmptyListHint();
        configureSelectedPrice();
        setDataForTabs();
    }

    private void changeFeatureImageAfterFiveMinutes() {
        final long currentTimeMillis = System.currentTimeMillis();
        if (Math.abs(currentTimeMillis - _lastFeatureImageChange) >= FIVE_MINUTES) {
            _featureImageHandler.switchFeatureImage();
            final int selectedTab = _menuTabHandler.getSelectedTab();
            _featureImageHandler.onPageSelected(selectedTab);
        }
    }

    private void preselectDay() {
        final Calendar calendar = Calendar.getInstance();
        final int dayInYear = calendar.get(Calendar.DAY_OF_YEAR);
        if (dayInYear != _lastPreselectionDay) {
            _lastPreselectionDay = dayInYear;
            final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
                case Calendar.TUESDAY:
                    switchTab(1);
                    break;
                case Calendar.WEDNESDAY:
                    switchTab(2);
                    break;
                case Calendar.THURSDAY:
                    switchTab(3);
                    break;
                case Calendar.FRIDAY:
                    switchTab(4);
                    break;
                default:
                    switchTab(0);
                    break;
            }
        }
    }

    private void switchTab(int tabIndex) {
        final int selectedTab = _menuTabHandler.getSelectedTab();
        if (tabIndex != selectedTab) {
            _menuTabHandler.onPageSelected(tabIndex);
        }
    }

    private void configureEmptyListHint() {
        final MenuTabAdapter menuTabAdapter = _menuTabHandler.getMenuTabAdapter();
        final boolean dataRequested = _dataProvider.hasRequestedData();
        if (dataRequested) {
            menuTabAdapter.setHint(WeekdayTabHint.NoMealData);
        } else {
            final boolean mensaSelected = _userPreferences.validator().hasMensaSelected();
            if (mensaSelected) {
                menuTabAdapter.setHint(WeekdayTabHint.NoDataRequested);
            } else {
                menuTabAdapter.setHint(WeekdayTabHint.NoMensaSelected);
            }
        }
    }

    private void configureSelectedPrice() {
        final MenuTabAdapter menuTabAdapter = _menuTabHandler.getMenuTabAdapter();
        final Integer priceGroupId = _userPreferences.getSelectedPriceId();
        final PriceGroup priceGroup;
        if (priceGroupId != null) {
            priceGroup = PriceGroup.findGroupForId(priceGroupId);
        } else {
            priceGroup = PriceGroup.guests;
        }
        menuTabAdapter.setPriceGroup(priceGroup);
    }

    private void setDataForTabs() {
        final MenuTabAdapter menuTabAdapter = _menuTabHandler.getMenuTabAdapter();
        final List<Day> data = _dataProvider.getData();
        menuTabAdapter.setData(data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_SETTINGS && data != null) {
            final boolean changedMensa = data.getBooleanExtra(SettingsActivity.CHANGED_MENSA, false);
            if (changedMensa) {
                updateMensaName();
                //todo request data
            }
            final boolean changedPrice = data.getBooleanExtra(SettingsActivity.CHANGED_PRICE, false);
            if (changedPrice) {
                configureSelectedPrice();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_overview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        final int id = item.getItemId();
        if (id == R.id.actionbar_view_settings) {
            handled = true;
            final Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, REQUEST_SETTINGS);
        } else if (id == R.id.actionbar_refresh) {
            handled = true;
            triggerRefresh();
        }
        return handled || super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        _featureImageHandler.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        _featureImageHandler.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        _featureImageHandler.onPageScrollStateChanged(state);
    }

    @Override
    public void onNetworkError() {

    }

    @Override
    public void onUnknownError() {

    }

    @Override
    public void onDataReceived(@NonNull List<Day> data) {
        configureEmptyListHint();
        setDataForTabs();
    }

    public void triggerRefresh() {
        _dataProvider.requestData(this);
    }
}
