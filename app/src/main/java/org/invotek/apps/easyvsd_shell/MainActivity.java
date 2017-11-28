package org.invotek.apps.easyvsd_shell;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public enum State{
        Navigate,
        HotspotEdit,
        Drawing,
        Erasing,
        AdjustingPauseTime;

        public static int getValue(State current)
        {
            switch(current){
                case Navigate:	return 0;
                case HotspotEdit: return 1;
                case Drawing: return 2;
                case Erasing: return 3;
                case AdjustingPauseTime: return 4;
                default: return 0;
            }
        }

        public static State getState(int value){
            switch(value)
            {
                case 0:	return Navigate;
                case 1:	return HotspotEdit;
                case 2:	return Drawing;
                case 3:	return Erasing;
                case 4: return AdjustingPauseTime;
                default: return Navigate;
            }
        }

        public static String toString(State current, Mode mode){
            switch(current){
                case Navigate:
                    String ret = "Navigating";
                    if(mode == Mode.Create)
                        ret = "Creating";
                    else if(mode == Mode.AdvancedCreate)
                        ret = "Creating Pro";
                    return ret;
                case HotspotEdit: return "Editing Hotspots";
                case Drawing: return "Drawing";
                case Erasing: return "Erasing";
                case AdjustingPauseTime: return "Adjusting Pause Time";
                default: return "Navigating";
            }
        }
    }

    public enum Mode{
        Navigate,
        iSnap,
        Create,
        AdvancedCreate;

        public static int getValue(Mode current)
        {
            switch(current){
                case Navigate:	return 0;
                case iSnap: return 1;
                case Create: return 2;
                case AdvancedCreate: return 3;
                default: return 0;
            }
        }

        public static Mode getMode(int value){
            switch(value)
            {
                case 0:	return Navigate;
                case 1:	return iSnap;
                case 2:	return Create;
                case 3:	return AdvancedCreate;
                default: return Navigate;
            }
        }

        public static String toString(Mode current){
            switch(current){
                case Navigate: return "Navigating";
                case iSnap: return "iSnap";
                case Create: return "Create";
                case AdvancedCreate: return "Advanced Create";
                default: return "Navigating";
            }
        }
    }

    AnimatingView animationViewRemoving, animationViewShowing;
    ImageView destinationView;
    SurfaceView videoSurface;
    SurfaceHolder videoHolder;
    HorizontalScrollView hActivities, hPages;
    ScrollView vActivities, vPages;
    LinearLayout activityLayout, pageLayout;
    NewDrawingView dvDrawing;
    GridLayout adminPanel;
    GridLayout navLayout;
    //HorizontalScrollView MainScrollView;

    BackgroundHighlightButton btnCreateActivity, btnCreatePage, btnEditHotspot, btnCancelHotspot, btnDrawingMode,
            btnNavMode, btnStartRecording, btnEndRecording, btnDeleteHotspot, /*btnUndo, btnRedo,*/ btnVideoRestart,
            btnVideoStepBack, btnVideoPlay, btnVideoStepForward, btnVideoPause;

    Drawing currentDrawing;
    int[][] userData = {{R.drawable.car_exterior, R.drawable.car_interior},
            {R.drawable.lake_house},
            {R.drawable.dog_group, R.drawable.standing_aussie}};
    int currentPage = -1;
    int currentActivity = -1;
    private int minimumPageId = 200;
    private int maximumPageId = 299;
    private int minimumActivityId = 100;
    private int maximumActivityId= 199;

    State currentState = State.Navigate;
    Mode currentMode = Mode.Navigate;

    private static Context context;
    public static Context getAppContext(){return context;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.MainLayout);
        InputMethodManager im = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);

        vActivities = (ScrollView)findViewById(R.id.ActivityGroups_vertical);
        if(vActivities != null){ vActivities.removeAllViews(); }

        hActivities = (HorizontalScrollView)findViewById(R.id.ActivityGroups_horizontal);
        if(hActivities != null){ hActivities.removeAllViews(); }

        loadUser();

        vPages = (ScrollView)findViewById(R.id.ActivityPages_vertical);
        //if(vPages != null){ vPages.removeAllViews(); }

        hPages = (HorizontalScrollView)findViewById(R.id.ActivityPages_horizontal);
        //if(hPages != null){ hPages.removeAllViews(); }

        destinationView = (ImageView)findViewById(R.id.ActivityImage);
        videoSurface = (SurfaceView)findViewById(R.id.ActivityVideo);
        videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Log.i("PlayTalk", "Main.surfaceCreated: " +
                //	" videoFilename=" + (TextUtils.isEmpty(videoFilename) ? "null/empty" : "OK") +
                //	", videoSurface=" + (videoSurface.getVisibility()==View.VISIBLE ? "Visible" : "Invisible") +
                //	", videoPlayer=" + (videoPlayer==null ? "null" : "notNull") );
//                prepVideo();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                //Log.i("PlayTalk", "Main.surfaceChanged, size=[" +
                //	Integer.toString(width) + "," +
                //	Integer.toString(height) + "]" +
                //	", videoFilename=" + (TextUtils.isEmpty(videoFilename) ? "null/empty" : "OK") +
                //	", videoSurface=" + (videoSurface.getVisibility()==View.VISIBLE ? "Visible" : "Invisible") +
                //	", videoPlayer=" + (videoPlayer==null ? "null" : "notNull") );
//                prepVideo();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //Log.i("PlayTalk", "Main.surfaceDestroyed entered, " +
                //	", videoFilename=" + (TextUtils.isEmpty(videoFilename) ? "null/empty" : "OK") +
                //	", videoSurface.Visibility=" +
                //	(videoSurface.getVisibility()==View.VISIBLE ? "Visible" : "Invisible") +
                //	", videoPlayer=" + (videoPlayer==null ? "null" : "notNull") );
                // Finished with this video playback, so release the media player
//                if( videoPlayer != null ) {
//                    videoPaused = true;
//                    videoPlayer.release();
//                    videoPlayer = null;
//                }
                //Log.i("PlayTalk", "Main.surfaceDestroyed exit, " +
                //	", videoPlayer=" + (videoPlayer==null ? "null" : "notNull") );
            }
        });

        SoftKeyboard softKeyboard = new SoftKeyboard(mainLayout, im);
        softKeyboard.setSoftKeyboardCallback(new SoftKeyboard.SoftKeyboardChanged()
        {

            @Override
            public void onSoftKeyboardHide()
            {
                int[] dimens = getImageSize();
                if((dvDrawing != null) && (currentDrawing != null)) {
                    imageSizeChanged = false;
                    dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                }
            }

            @Override
            public void onSoftKeyboardShow()
            {
                // Code here
            }
        });
        dvDrawing = (NewDrawingView)findViewById(R.id.dvDrawingView);
        Drawing.loadColors(getResources());
        dvDrawing.addDrawingViewListener(new NewDrawingView.NewDrawingViewListener(){
            @Override
            public void onEditingFinished() {
                dvDrawing.setDrawingInfo(getCurrentDrawing());
                setUpAdminPanel(false, false, false);
            }
        });


        animationViewRemoving = (AnimatingView)findViewById(R.id.AnimationViewRemoving);
        animationViewRemoving.addAnimatingtViewListener(new AnimatingView.AnimatingViewListener(){
            @Override
            public void onAnimationFinished() {
                setUpAdminPanel(false, false, false);
                System.gc();
            }

            @Override
            public void onAnimationStarted() {
                destinationView.setVisibility(View.VISIBLE);

                // Since this view only removes image, clear all currentPage info before starting to animate
                clearCurrentImage();
                hideOverlays();
                // Hide the video Surface too. This will also destroy the videoPlayer.
                videoSurface.setVisibility(View.INVISIBLE);

            }
        });
        animationViewShowing = (AnimatingView)findViewById(R.id.AnimationViewShowing);
        animationViewShowing.addAnimatingtViewListener(new AnimatingView.AnimatingViewListener(){
            @Override
            public void onAnimationFinished() {
                // Since this view only shows image, must load hotspots and drawing on top
                if((currentPage > -1)){
                    setActiveImage();
                    showOverlays(savedImageSize);
                }
                setUpAdminPanel(false, false, false);
                System.gc();
            }

            @Override
            public void onAnimationStarted() {
                // Since this view only shows image, no need to do anything when animation is starting.
                System.gc();
            }
        });

        LayoutTransition lt = mainLayout.getLayoutTransition();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            lt.enableTransitionType(LayoutTransition.CHANGING);
            lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
            lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            lt.enableTransitionType(LayoutTransition.APPEARING);
            lt.enableTransitionType(LayoutTransition.DISAPPEARING);
        }
        lt.setDuration(1000);
        lt.addTransitionListener(new LayoutTransition.TransitionListener(){

            @Override
            public void endTransition(LayoutTransition transition,
                                      ViewGroup container, View view, int transitionType) {
                //Log.i("PlayTalk", "Main.onPreExecute: EndTransition type=" + transitionType);
                int[] dimens = getImageSize();
                switch(transitionType){
                    case LayoutTransition.APPEARING:
                    case LayoutTransition.CHANGE_APPEARING:
                        if((!animationViewShowing.animating) &&  (view == adminPanel)){
                            if(imageSizeChanged && (dvDrawing != null)) {
                                if((currentPage >= 0)){
                                    imageSizeChanged = false;
                                    dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                                }
                            }
                            if(currentActivity >= 0) {
                                ((BackgroundHighlightButton) activityLayout.getChildAt(currentActivity)).
                                        setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                            }if(currentPage >= 0) {
                                ((BackgroundHighlightButton) pageLayout.getChildAt(currentPage)).
                                        setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                            }
                        }else if(imageSizeChanged && (dvDrawing != null)) {
                            if((currentPage >= 0)){
                                imageSizeChanged = false;
                                dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                            }
                        }
                        if(currentActivity >= 0) {
                            ((BackgroundHighlightButton) activityLayout.getChildAt(currentActivity)).
                                    setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                        }if(currentPage >= 0) {
                        ((BackgroundHighlightButton) pageLayout.getChildAt(currentPage)).
                                setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                    }
                        break;
                    case LayoutTransition.DISAPPEARING:
                    case LayoutTransition.CHANGE_DISAPPEARING:
                        //Log.i("PlayTalk", "Main.onPreExecute: EndTransition Disappearing");
                        if((!animationViewShowing.animating) &&  (view == adminPanel)){
                            if(imageSizeChanged && (dvDrawing != null)) {
                                if((currentPage >= 0)){
                                    imageSizeChanged = false;
                                    dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                                }
                            }
                        }else if(imageSizeChanged && (dvDrawing != null)) {
                            if((currentPage >= 0)){
                                imageSizeChanged = false;
                                dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                            }
                        }
                        if(currentActivity >= 0) {
                            ((BackgroundHighlightButton) activityLayout.getChildAt(currentActivity)).
                                    setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                        }if(currentPage >= 0) {
                        ((BackgroundHighlightButton) pageLayout.getChildAt(currentPage)).
                                setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                        }
                        break;
                    case LayoutTransition.CHANGING:
                        //Log.i("PlayTalk", "Main.onPreExecute: EndTransition Changing");
                        if(imageSizeChanged && (dvDrawing != null)) {
                            if((currentPage >= 0)){
                                imageSizeChanged = false;
                                dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                            }
                        }
                        if(currentActivity >= 0) {
                            ((BackgroundHighlightButton) activityLayout.getChildAt(currentActivity)).
                                    setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                        }if(currentPage >= 0) {
                        ((BackgroundHighlightButton) pageLayout.getChildAt(currentPage)).
                                setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                        }
                        break;
                    default:

                        break;
                }
            }

            @Override
            public void startTransition(LayoutTransition transition,
                                        ViewGroup container, View view, int transitionType) {
                // Not doing anything on startTransition right now
            }

        });
        mainLayout.setLayoutTransition(lt);
        //}
        navLayout = (GridLayout)findViewById(R.id.NavOptions);
        navLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){

            @Override
            public void onLayoutChange(View v, int left, int top,
                                       int right, int bottom, int oldLeft, int oldTop,
                                       int oldRight, int oldBottom) {
                Log.d("Pages", "navLayout.onLayoutChange: height=" + v.getHeight() + " width=" + v.getWidth() + " new=[" +
                        left + "," + top + "," + right + "," + bottom + "] old=[" + oldLeft + "," + oldTop + "," + oldRight +
                        "," + oldBottom + "]");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if(bottom > oldBottom)
                    setUpAdminPanel(false, false, false);
            }});


        initAdminPanel();
        initDrawingOptions();
    }



    @SuppressLint("CutPasteId")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("LifeCycle", "onConfiguruationChanged");

        vActivities = (ScrollView)findViewById(R.id.ActivityGroups_vertical);
        if(vActivities != null){ vActivities.removeAllViews(); }
        hActivities = (HorizontalScrollView)findViewById(R.id.ActivityGroups_horizontal);
        if(hActivities != null){ hActivities.removeAllViews(); }

        loadUser();

        vPages = (ScrollView)findViewById(R.id.ActivityPages_vertical);
        //if(vPages != null){vPages.removeAllViews();}
        hPages = (HorizontalScrollView)findViewById(R.id.ActivityPages_horizontal);
        //if(hPages != null){ hPages.removeAllViews(); }

        setUpAdminPanel(false, false, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.modeSwitch:
                if(currentMode == Mode.AdvancedCreate)
                    setMode(Mode.Navigate, false, false);
                else
                    setMode(Mode.AdvancedCreate, false, false);
                setUpAdminPanel(false, false, false);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View clickedView) {
        int currentId = clickedView.getId();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor prefsEdit = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }
        int newColor;
        switch(currentId){
            //case R.id.StylusES:
            //case R.id.StylusS:
            case R.id.StylusM:
            //case R.id.StylusL:
            case R.id.StylusEL:
                ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);
                ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);
                float newWidth;
                switch(currentId){
                    case R.id.StylusM:
                        newWidth = 15;
                        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                        break;
                    case R.id.StylusEL:
                        newWidth = 50; //25;
                        ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                        break;
                    default:
                        newWidth = 15;
                        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                        break;
                }
                currentDrawing.setStrokeWidth(newWidth);
                prefsEdit.putFloat("stylus_width", newWidth).commit();
                break;
            case R.id.StylusErase:
                setDrawingOptions(true, currentState != State.Erasing);
                break;
            case R.id.ClearDrawing:
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
                confirmBuilder.setTitle("Confirm Delete");
                confirmBuilder.setMessage("Are you sure that you want to clear the drawings on this page?");
                confirmBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dvDrawing.clearDrawing();
                        dialog.dismiss();
                    }
                });
                confirmBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // just return
                        dialog.dismiss();
                    }
                });
                AlertDialog confirmAlert = confirmBuilder.create();
                confirmAlert.show();
                break;
            case R.id.ColorBlack:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.black);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorBlue:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.blue);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorLimeGreen:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.limegreen);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorPink:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.pink);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorPurple:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.purple);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorRed:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.red);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorWhite:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.white);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.ColorYellow:
                if(currentState == State.Erasing)
                    setDrawingOptions(true, false);

                newColor = getResources().getColor(R.color.yellow);
                if(currentDrawing != null)
                {
                    currentDrawing.setColor(newColor);
                }
                setActiveColor(false);
                prefsEdit.putInt("stylus_color", getIndexFromId(currentId)).commit();
                break;
            case R.id.btn_newActivity:
                // create new activity by taking picture
                break;
            case R.id.btn_newPage:
                // create new page by taking picture
                break;
            case R.id.btn_editHotspot:
                // enter create hotspot mode
                // setDrawingOptions(false, false);
                break;
            case R.id.btn_cancelHotspot:
                // get out of create hotspot mode
                // setDrawingOptions(false, false);
                break;
            case R.id.btn_drawingMode:
                setDrawingOptions(true, false);
                break;
            case R.id.btn_navMode:
                setDrawingOptions(false, false);
                loadUser();
                loadGroup();
                break;
            case R.id.btn_startRecording:
                // start recording audio for new hotspot
                break;
            case R.id.btn_endRecording:
                // end recording of audio for new hotspot
                break;
            case R.id.btn_deleteHotspot:
                // delete currently selected hotspot
                break;
            /*
            case R.id.btn_undo:
                dvDrawing.undo();
                setUpAdminPanel(false, false, false);
                break;
            case R.id.btn_redo:
                dvDrawing.redo();
                setUpAdminPanel(false, false, false);
                break;
            */
            case R.id.btn_video_restart:
                // restart video
                break;
            case R.id.btn_video_skipback:
                // skip backward video
                break;
            case R.id.btn_video_play:
                // play video
                break;
            case R.id.btn_video_skipforward:
                // skip forward video
                break;
            case R.id.btn_video_pause:
                // pause video
                break;
            default:
                // this is where pages and groups are selected
                if(currentId >= minimumActivityId && currentId < maximumActivityId){
                    int clickedActivity = currentId - minimumActivityId;
                    if(currentActivity < 0){
                        // No Activity was selected before this click
                        currentActivity = clickedActivity;
                        currentPage = 0;
                        ((BackgroundHighlightButton)activityLayout.getChildAt(currentActivity)).
                                setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                        loadGroup();
                        showImageView(clickedView);
                    }else if(clickedActivity == currentActivity){
                        // Current activity has been clicked
                        // No Activity was selected before this click
                        ((BackgroundHighlightButton)activityLayout.getChildAt(currentActivity)).
                                setState(BackgroundHighlightButton.State.NORMAL);
                        if(currentPage >= 0){ hideImageView(clickedView); }
                        currentActivity = -1;
                        currentPage = -1;
                        loadGroup();
                    }else{
                        // clicked activity is different than the current activity
                        // No Activity was selected before this click
                        ((BackgroundHighlightButton)activityLayout.getChildAt(currentActivity)).
                                setState(BackgroundHighlightButton.State.NORMAL);
                        if(currentPage >= 0){ hideImageView(clickedView); }
                        currentActivity = clickedActivity;
                        currentPage = 0;
                        ((BackgroundHighlightButton)activityLayout.getChildAt(currentActivity)).
                                setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                        loadGroup();
                        showImageView(clickedView);
                    }
                }else if(currentId >= minimumPageId && currentId < maximumPageId){
                    int clickedPage = currentId - minimumPageId;
                    if(currentPage < 0){
                     // No page was selected before this click
                        currentPage = clickedPage;
                        loadGroup();
                        showImageView(clickedView);
                    }else if(clickedPage == currentPage){
                        // Current page has been clicked
                        hideImageView(clickedView);
                        currentPage = -1;
                        loadGroup();
                    }else{
                        // clicked page is different than the current page
                        hideImageView(clickedView);
                        currentPage = clickedPage;
                        loadGroup();
                        showImageView(clickedView);
                    }
                }
                break;
        }
    }

    private void loadGroup(){
        if(pageLayout == null)
            pageLayout = new LinearLayout(MainActivity.this);
        pageLayout.setBackgroundResource(R.color.silver);
        if(vPages != null){
            // In landscape layout
            vPages.removeAllViews();
            vPages.setVisibility(View.GONE);
            pageLayout.removeAllViews();
            if(currentActivity >= 0) {
                pageLayout.setOrientation(LinearLayout.VERTICAL);
                BackgroundHighlightButton button;
                for (int i = 0; i < userData[currentActivity].length; ++i){
                    button = getButton(currentActivity, i, false);
                    if(currentPage == i)
                        button.setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                    pageLayout.addView(button);
                }
                vPages.addView(pageLayout);
                vPages.setVisibility(View.VISIBLE);
            }
        }
        if(hPages != null){
            // In portrait layout
            hPages.removeAllViews();
            hPages.setVisibility(View.GONE);
            pageLayout.removeAllViews();
            if(currentActivity >= 0) {
                pageLayout.setOrientation(LinearLayout.HORIZONTAL);
                BackgroundHighlightButton button;
                for (int i = 0; i < userData[currentActivity].length; ++i){
                    button = getButton(currentActivity, i, false);
                    if(currentPage == i)
                        button.setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                    pageLayout.addView(button);
                }

                hPages.addView(pageLayout);
                hPages.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadUser(){
        if(activityLayout == null)
            activityLayout = new LinearLayout(MainActivity.this);
        activityLayout.setBackgroundResource(R.color.silver);
        if(vActivities != null){
            vActivities.removeAllViews();
            if(activityLayout.getChildCount() <= 0){
                BackgroundHighlightButton button;
                for (int i = 0; i < userData.length; ++i) {
                    button = getButton(i, 0, true);
                    if(currentActivity >= 0)
                        button.setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                    activityLayout.addView(button);
                }
            }
            activityLayout.setOrientation(LinearLayout.VERTICAL);
            vActivities.addView(activityLayout);
            vActivities.setVisibility(View.VISIBLE);
        }else if(hActivities != null){
            hActivities.removeAllViews();
            if(activityLayout.getChildCount() <= 0){
                BackgroundHighlightButton button;
                for (int i = 0; i < userData.length; ++i) {
                    button = getButton(i, 0, true);
                    if (currentActivity >= 0)
                        button.setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                    activityLayout.addView(button);
                }
            }
            activityLayout.setOrientation(LinearLayout.HORIZONTAL);
            hActivities.addView(activityLayout);
            hActivities.setVisibility(View.VISIBLE);
        }
    }


    private BackgroundHighlightButton getButton(int activityIndex, int pageIndex, boolean isActivity){
        BackgroundHighlightButton button = new BackgroundHighlightButton(MainActivity.this);
        button.setId((isActivity ? minimumActivityId + activityIndex : minimumPageId + pageIndex));
        DisplayMetrics metrics = MainActivity.this.getResources().getDisplayMetrics();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250"));//Button on left size
        buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        Log.d("ActivityPage", "buttonSize=[" + String.valueOf(buttonSize) + "]");
        int margin = (int)Math.round(buttonSize / 25f);
        button.setOnClickListener(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        lp.setMargins(margin, margin, margin, margin);
        lp.width = buttonSize;
        lp.height = buttonSize;
        button.setLayoutParams(lp);
        button.setMinimumHeight(buttonSize);
        button.setMinimumWidth(buttonSize);
        button.setMaxHeight(buttonSize);
        button.setMaxWidth(buttonSize);
//        ImageDecoder decoder = new ImageDecoder();
//        button.setForegroundBitmap(decoder.getImage(MainActivity.this,
//                buttonSize,
//                buttonSize,
//                true,
//                userData[activityIndex][pageIndex]),
//                false);
        button.setForegroundImageResource(MainActivity.this,
                userData[activityIndex][pageIndex],
                false);
        return button;
    }

    private void hideImageView(View clickedView){
        destinationView.setVisibility(View.VISIBLE);
        videoSurface.setVisibility(View.INVISIBLE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        ImageDecoder imageDecoder = new ImageDecoder();
        Bitmap toHide = imageDecoder.getImage(this,
                savedImageSize[0],
                savedImageSize[1],
                false,
                userData[currentActivity][currentPage]);

        float stepsToAnimate = Integer.parseInt(prefs.getString("animation_time", "100"));
        float heightDividedWidth;
        float change, height;
        Rect visibleSourceRect = new Rect();
        Rect visibleDestRect = new Rect();
        destinationView.getGlobalVisibleRect(visibleSourceRect);
        clickedView.getGlobalVisibleRect(visibleDestRect);
        //Log.i("PlayTalk", "Main.hideImageView entry" +
        //	", SourceRect=" + visibleSourceRect.toShortString() +
        //	", DestRect=" + visibleDestRect.toShortString());
        float visibleDestHeight = visibleDestRect.height() > 0 ? (float)visibleDestRect.height():(float)1;
        float visibleDestWidth = visibleDestRect.width() > 0? (float)visibleDestRect.width():(float)1;
        heightDividedWidth = visibleDestHeight/visibleDestWidth;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            height = visibleSourceRect.width() * heightDividedWidth;
            change = visibleSourceRect.height() - height;
            visibleSourceRect.top += change/2;
            visibleSourceRect.bottom -= change/2;
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            height = visibleSourceRect.height() / heightDividedWidth;
            change = visibleSourceRect.width() - height;
            visibleSourceRect.left += change/2;
            visibleSourceRect.right -= change/2;
        }
        //Log.i("PlayTalk", "Main.hideImageView exit" +
        //	", SourceRect=" + visibleSourceRect.toShortString() +
        //	", DestRect=" + visibleDestRect.toShortString());


        animationViewRemoving.startAnimation(visibleSourceRect.left, visibleSourceRect.top,
                visibleSourceRect.right, visibleSourceRect.bottom, visibleDestRect.left, visibleDestRect.top,
                visibleDestRect.right, visibleDestRect.bottom, stepsToAnimate, toHide,
                this, getActionBarHeight());
    }

    private void showImageView(View clickedView){
        destinationView.setVisibility(View.VISIBLE);

        if(clickedView != null){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            float stepsToAnimate = Integer.parseInt(prefs.getString("animation_time", "100"));
            float heightDividedWidth;
            float change, height;
            Rect visibleSourceRect = new Rect();
            Rect visibleDestRect = new Rect();
            clickedView.getGlobalVisibleRect(visibleSourceRect);
            destinationView.getGlobalVisibleRect(visibleDestRect);
            Log.i("PlayTalk", "Main.showImageView entry" +
            	", SourceRect=" + visibleSourceRect.toShortString() +
            	", DestRect=" + visibleDestRect.toShortString() );
            float visibleSourceWidth = visibleSourceRect.width() > 0 ? (float)visibleSourceRect.width() : (float)1;
            float visibleSourceHeight = visibleSourceRect.height() > 0 ? (float)visibleSourceRect.height() : (float)1;
            heightDividedWidth = visibleSourceHeight/ visibleSourceWidth;
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                height = visibleDestRect.width() * heightDividedWidth;
                change = visibleDestRect.height() - height;
                visibleDestRect.top += change/2;
                visibleDestRect.bottom -= change/2;
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                height = visibleDestRect.height() / heightDividedWidth;
                change = visibleDestRect.width() - height;
                visibleDestRect.left += change/2;
                visibleDestRect.right -= change/2;
            }
            Log.i("PlayTalk", "Main.showImageView exit" +
            	", SourceRect=" + visibleSourceRect.toShortString() +
            	", DestRect=" + visibleDestRect.toShortString() );

            Bitmap toShow = ((BackgroundHighlightButton)clickedView).getButtonBitmap();

            animationViewShowing.startAnimation(visibleSourceRect.left, visibleSourceRect.top,
                    visibleSourceRect.right, visibleSourceRect.bottom, visibleDestRect.left, visibleDestRect.top,
                    visibleDestRect.right, visibleDestRect.bottom, stepsToAnimate, toShow,
                    this, getActionBarHeight());
        }
    }

    public void setActiveImage(){
        if(currentPage < 0) {
            clearCurrentImage();
        }
        else{
            int[] dimens = getImageSize();
           // int[] dimens;
            //dimens = new int[2];
           // dimens[0] = 45;
            //[0] = 45;

            // Load the still image for this page into destinationView (ImageView)
            ImageDecoder decoder = new ImageDecoder();
            destinationView.setImageBitmap(decoder.getImage(this,
                    dimens[0],
                    dimens[1],
                    false,
                    userData[currentActivity][currentPage]));
            dimens = getImageSize(); // must call again once image is set because this resizes destination view
            //Log.i("PlayTalk", "Main.setActiveImage: dimens=" + formatIntegerArray(dimens) );


            // No videoFilename, so still image only
            videoSurface.setVisibility(View.INVISIBLE);
            videoPaused = true;

            // Show the Bitmap
            destinationView.setVisibility(View.VISIBLE);

            //Log.i("PlayTalk", "Main.setActiveImage: videoSurface is INVISIBLE");
            // Setup the drawings and hotspots associated with this image
            // Show the drawings and hotspots for this PausePoint (only one PausePoint per still image)
            //updateOverlays( page, dimens );
            dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));


            Log.d("State", "setActiveImage: call setDrawingOptions");
            setDrawingOptions((currentState == State.Drawing) || (currentState == State.Erasing) || (currentMode == Mode.iSnap), currentState == State.Erasing);
        }

    }

    private Drawing getCurrentDrawing(){
        if(currentDrawing == null) {
            currentDrawing = new Drawing(MainActivity.this.getResources(),
                    savedImageSize[0], savedImageSize[1]);
        }
        return currentDrawing;
    }

    private void showOverlays( int[] dimens ) {
        //Log.i("PlayTalk", "Main.showOverlays: " +
        //	", pageId=" + Integer.toString(page.getId()) +
        //	", PausePoint at " + Integer.toString(page.getCurrentPausePoint().getPauseTime()) +
        //	", dimens=" + formatIntegerArray(dimens) );
        dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
    }

    private void hideOverlays( ) {
        // Undisplay the drawings and hotspots
        //Log.i("PlayTalk", "Main.hideOverlays" +
        //	", pageId=" + Integer.toString(currentUser.getCurrentPage().getId()) +
        //	", PausePoint at " + Integer.toString(currentUser.getCurrentPage().getCurrentPausePoint().getPauseTime()) +
        //	", imageSize=" + formatIntegerArray(getImageSize()) );
        if(dvDrawing != null)
            dvDrawing.setDrawingInfo(null);

        // Unload the drawings and hotspots from memory
        getCurrentDrawing().unloadDrawing();
    }

    public void clearCurrentImage(){
        Drawable drawable = destinationView.getDrawable();
        if(drawable instanceof BitmapDrawable){
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
            bitmap = null;
        }
        destinationView.setImageDrawable(null);
        destinationView.setImageBitmap(null);
        destinationView.setImageResource(android.R.color.transparent);
        destinationView.destroyDrawingCache();
        if(dvDrawing != null)
            dvDrawing.setDrawingInfo(null);

        videoSurface.setVisibility(View.INVISIBLE);

        System.gc();
        Runtime.getRuntime().gc();
    }

    int getActionBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        TypedValue tv = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);

        return statusBarHeight + actionBarHeight;
    }

    private void initAdminPanel(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        boolean onTopOfVSD = false; // prefs.getBoolean("me2_cover_space", true);
        int buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250"));
        buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        int margin = (int)Math.round(buttonSize / 25f);
        adminPanel = (GridLayout)findViewById(R.id.adminPanel);
        LinearLayout.LayoutParams pnlLayoutParamsAdmin = (LinearLayout.LayoutParams) adminPanel.getLayoutParams();
        LinearLayout.LayoutParams pnlLayoutParamsDestination = (LinearLayout.LayoutParams)destinationView.getLayoutParams();
        LinearLayout.LayoutParams pnlLayoutParamsVideoSurface = (LinearLayout.LayoutParams)videoSurface.getLayoutParams();
        LinearLayout.LayoutParams pnlLayoutParamsDrawings = (LinearLayout.LayoutParams)dvDrawing.getLayoutParams();


         if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            /*if(onTopOfVSD){
                pnlLayoutParamsDestination.addRule(RelativeLayout.LEFT_OF, 0);
                pnlLayoutParamsVideoSurface.addRule(RelativeLayout.LEFT_OF, 0);
                pnlLayoutParamsDrawings.addRule(RelativeLayout.LEFT_OF, 0);
            }else{
                pnlLayoutParamsDestination.addRule(RelativeLayout.LEFT_OF, R.id.adminPanel);
                pnlLayoutParamsVideoSurface.addRule(RelativeLayout.LEFT_OF, R.id.adminPanel);
                pnlLayoutParamsDrawings.addRule(RelativeLayout.LEFT_OF, R.id.adminPanel);
            }*/
            pnlLayoutParamsAdmin.width = (buttonSize + (margin * 4));
        }else{
            /*
            if(onTopOfVSD){
                pnlLayoutParamsDestination.addRule(RelativeLayout.ABOVE, 0);
                pnlLayoutParamsVideoSurface.addRule(RelativeLayout.ABOVE, 0);
                pnlLayoutParamsDrawings.addRule(RelativeLayout.ABOVE, 0);
            }else{
                pnlLayoutParamsDestination.addRule(RelativeLayout.ABOVE, R.id.adminPanel);
                pnlLayoutParamsVideoSurface.addRule(RelativeLayout.ABOVE, R.id.adminPanel);
                pnlLayoutParamsDrawings.addRule(RelativeLayout.ABOVE, R.id.adminPanel);
            }
            */
            pnlLayoutParamsAdmin.height = (buttonSize + (margin * 4));

        }

        adminPanel.setLayoutParams(pnlLayoutParamsAdmin);
        destinationView.setLayoutParams(pnlLayoutParamsDestination);
        videoSurface.setLayoutParams(pnlLayoutParamsVideoSurface);
        dvDrawing.setLayoutParams(pnlLayoutParamsDrawings);

        btnCreateActivity = (BackgroundHighlightButton)findViewById(R.id.btn_newActivity);
        btnCreateActivity.setForegroundImageResource(this, R.drawable.new_activity_circle, true); //realImages ? R.drawable.camera_me2 : R.drawable.take_picture, true);
        btnCreateActivity.setOnClickListener(this);
        setUpAdminButton(btnCreateActivity, buttonSize, margin);
        btnCreatePage = (BackgroundHighlightButton)findViewById(R.id.btn_newPage);
        btnCreatePage.setForegroundImageResource(this, R.drawable.new_page_circle, true); // realImages ? R.drawable.camera_me2 : R.drawable.take_picture, true);
        btnCreatePage.setOnClickListener(this);
        setUpAdminButton(btnCreatePage, buttonSize, margin);
        btnEditHotspot = (BackgroundHighlightButton)findViewById(R.id.btn_editHotspot);
        btnEditHotspot.setForegroundImageResource(this, R.drawable.hotspot_circle, true); // realImages ? R.drawable.hotspot_icon : R.drawable.create_hotspot, true);
        btnEditHotspot.setOnClickListener(this);
        setUpAdminButton(btnEditHotspot, buttonSize, margin);
        btnCancelHotspot = (BackgroundHighlightButton)findViewById(R.id.btn_cancelHotspot);
        btnCancelHotspot.setForegroundImageResource(this, R.drawable.cancel_circle, true); // realImages ? R.drawable.cancel : R.drawable.cancel_hotspot, true);
        btnCancelHotspot.setOnClickListener(this);
        setUpAdminButton(btnCancelHotspot, buttonSize, margin);
        btnDrawingMode = (BackgroundHighlightButton)findViewById(R.id.btn_drawingMode);
        btnDrawingMode.setForegroundImageResource(this, R.drawable.draw_circle, true); // realImages ? R.drawable.drawing_me2 : R.drawable.start_drawing, true);
        //btnDrawingMode.setBackgroundImageResource(this, R.drawable.undo_icon, true);
        btnDrawingMode.setOnClickListener(this);
        setUpAdminButton(btnDrawingMode, buttonSize, margin);
        btnNavMode = (BackgroundHighlightButton)findViewById(R.id.btn_navMode);
        btnNavMode.setForegroundImageResource(this, R.drawable.return_circle, true); // realImages ? R.drawable.navigate : R.drawable.start_navigation, true);
        btnNavMode.setOnClickListener(this);
        setUpAdminButton(btnNavMode, buttonSize, margin);
        btnStartRecording = (BackgroundHighlightButton)findViewById(R.id.btn_startRecording);
        btnStartRecording.setForegroundImageResource(this, R.drawable.record_circle, true); // realImages ? R.drawable.record_icon : R.drawable.start_recording, true);
        btnStartRecording.setOnClickListener(this);
        setUpAdminButton(btnStartRecording, buttonSize, margin);
        btnEndRecording = (BackgroundHighlightButton)findViewById(R.id.btn_endRecording);
        btnEndRecording.setForegroundImageResource(this, R.drawable.stop_circle, true); // realImages ? R.drawable.stop : R.drawable.end_recording, true);
        btnEndRecording.setOnClickListener(this);
        setUpAdminButton(btnEndRecording, buttonSize, margin);
        btnDeleteHotspot = (BackgroundHighlightButton)findViewById(R.id.btn_deleteHotspot);
        btnDeleteHotspot.setForegroundImageResource(this, R.drawable.delete_circle, true); // realImages ? R.drawable.trash_icon : R.drawable.delete_hotspot, true);
        btnDeleteHotspot.setOnClickListener(this);
        setUpAdminButton(btnDeleteHotspot, buttonSize, margin);
        /*
        btnUndo = (BackgroundHighlightButton)findViewById(R.id.btn_undo);
        btnUndo.setForegroundImageResource(this, R.drawable.undo_circle, true);
        btnUndo.setOnClickListener(this);
        setUpAdminButton(btnUndo, buttonSize, margin);
        btnRedo = (BackgroundHighlightButton)findViewById(R.id.btn_redo);
        btnRedo.setForegroundImageResource(this, R.drawable.redo_circle, true);
        btnRedo.setOnClickListener(this);
        setUpAdminButton(btnRedo, buttonSize, margin);
        */
        btnVideoRestart = (BackgroundHighlightButton)findViewById(R.id.btn_video_restart);
        btnVideoRestart.setForegroundImageResource(this, R.drawable.restart_button, true);
        btnVideoRestart.setOnClickListener(this);
        setUpAdminButton(btnVideoRestart, buttonSize, margin);
        btnVideoStepBack = (BackgroundHighlightButton)findViewById(R.id.btn_video_skipback);
        btnVideoStepBack.setForegroundImageResource(this, R.drawable.skip_back_button, true);
        btnVideoStepBack.setOnClickListener(this);
        setUpAdminButton(btnVideoStepBack, buttonSize, margin);
        btnVideoPlay = (BackgroundHighlightButton)findViewById(R.id.btn_video_play);
        btnVideoPlay.setForegroundImageResource(this, R.drawable.play_button, true);
        btnVideoPlay.setOnClickListener(this);
        setUpAdminButton(btnVideoPlay, buttonSize, margin);
        btnVideoStepForward = (BackgroundHighlightButton)findViewById(R.id.btn_video_skipforward);
        btnVideoStepForward.setForegroundImageResource(this, R.drawable.skip_forward_button, true);
        btnVideoStepForward.setOnClickListener(this);
        setUpAdminButton(btnVideoStepForward, buttonSize, margin);
        btnVideoPause = (BackgroundHighlightButton)findViewById(R.id.btn_video_pause);
        btnVideoPause.setForegroundImageResource(this, R.drawable.pause_button, true);
        btnVideoPause.setOnClickListener(this);
        setUpAdminButton(btnVideoPause, buttonSize, margin);
    }

    private void setUpAdminButton(Button toSet, int size, int margin){
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.setMargins(margin, margin, margin, margin);
        lp.width = size;
        lp.height = size;
        toSet.setLayoutParams(lp);
        toSet.setMinimumHeight(size);
        toSet.setMinimumWidth(size);
        toSet.setMaxHeight(size);
        toSet.setMaxWidth(size);
    }

    boolean isVideo = false; // Trick the system into displaying icons as though current page is a video
    int pauseLocation = 0; // When isVideo = true: 0 = first pause, 1 = middle pause, 2 = last pause
    private boolean videoPaused;
    private void setUpAdminPanel(boolean hotspotCreated, boolean hotspotSelected, boolean recording){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        boolean adminInNav = false; //prefs.getBoolean("admin_in_nav", true);
        DisplayMetrics metrics = MainActivity.this.getResources().getDisplayMetrics();
        boolean portrait = (MainActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ||
                (metrics.heightPixels > metrics.widthPixels);
        int buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250"));
        buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        if(navLayout == null){return;}
        ViewGroup.LayoutParams nP = navLayout.getLayoutParams();
        int margin = (int)Math.round(buttonSize / 25f);
        if(portrait){
            nP.height = (2 * buttonSize) + (4 * margin);
        }else{
            nP.width = (2 * buttonSize) + (4 * margin);
        }
        setUpAdminButton(btnCreateActivity, buttonSize, margin);
        setUpAdminButton(btnCreatePage, buttonSize, margin);
        setUpAdminButton(btnEditHotspot, buttonSize, margin);
        setUpAdminButton(btnCancelHotspot, buttonSize, margin);
        setUpAdminButton(btnDeleteHotspot, buttonSize, margin);
        setUpAdminButton(btnStartRecording, buttonSize, margin);
        setUpAdminButton(btnEndRecording, buttonSize, margin);
        setUpAdminButton(btnDrawingMode, buttonSize, margin);
        setUpAdminButton(btnNavMode, buttonSize, margin);
        //setUpAdminButton(btnUndo, buttonSize, margin);
        //setUpAdminButton(btnRedo, buttonSize, margin);
        setUpAdminButton(btnVideoRestart, buttonSize, margin);
        setUpAdminButton(btnVideoStepBack, buttonSize, margin);
        setUpAdminButton(btnVideoPlay, buttonSize, margin);
        setUpAdminButton(btnVideoStepForward, buttonSize, margin);
        setUpAdminButton(btnVideoPause, buttonSize, margin);
        navLayout.setLayoutParams(nP);
        if(navLayout.getChildCount() > 0){
            navLayout.removeView(btnCreateActivity);
            navLayout.removeView(btnCreatePage);
            navLayout.removeView(btnEditHotspot);
            navLayout.removeView(btnCancelHotspot);
            navLayout.removeView(btnDeleteHotspot);
            navLayout.removeView(btnStartRecording);
            navLayout.removeView(btnEndRecording);
            navLayout.removeView(btnDrawingMode);
            navLayout.removeView(btnNavMode);
            //navLayout.removeView(btnUndo);
            //navLayout.removeView(btnRedo);
            navLayout.removeView(btnVideoRestart);
            navLayout.removeView(btnVideoStepBack);
            navLayout.removeView(btnVideoPlay);
            navLayout.removeView(btnVideoStepForward);
            navLayout.removeView(btnVideoPause);
        }
        int newDimen = portrait ? navLayout.getWidth() : navLayout.getHeight();
        GridLayout.LayoutParams params;
        if(adminInNav && ((currentMode == Mode.Create) || (currentMode == Mode.AdvancedCreate) || (currentMode == Mode.iSnap))){
            //MainScrollView.onTouchEvent(false);
            if(adminPanel.getChildCount() > 0){
                adminPanel.removeView(btnCreateActivity);
                adminPanel.removeView(btnCreatePage);
                adminPanel.removeView(btnEditHotspot);
                adminPanel.removeView(btnCancelHotspot);
                adminPanel.removeView(btnDeleteHotspot);
                adminPanel.removeView(btnStartRecording);
                adminPanel.removeView(btnEndRecording);
                adminPanel.removeView(btnDrawingMode);
                adminPanel.removeView(btnNavMode);
                //adminPanel.removeView(btnUndo);
                //adminPanel.removeView(btnRedo);
                adminPanel.removeView(btnVideoRestart);
                adminPanel.removeView(btnVideoStepBack);
                adminPanel.removeView(btnVideoPlay);
                adminPanel.removeView(btnVideoStepForward);
                adminPanel.removeView(btnVideoPause);
                adminPanel.setVisibility(View.GONE);
            }
            if(currentState == State.Navigate){
                if(currentPage >= 0 && isVideo &&
                        !prefs.getBoolean("enableVideoControlHotspots", false) && !videoPaused){
                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, portrait ? 2 : 1, GridLayout.CENTER),
                            GridLayout.spec(portrait ? 2 : 0, portrait ? 1 : 2, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    if(portrait){
                        btnVideoPause.setHeight(navLayout.getHeight());
                        params.height = navLayout.getHeight();
                        params.width = buttonSize;
                    }else{
                        btnVideoPause.setWidth(navLayout.getWidth());
                        params.width = navLayout.getWidth();
                        params.height = buttonSize;
                    }
                    navLayout.addView(btnVideoPause, params);
                    newDimen -= (1 * (buttonSize + (2*margin)));
                }else{
                    params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER), GridLayout.spec(0, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    navLayout.addView(btnCreateActivity, params);
                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1: 0, GridLayout.CENTER), GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    navLayout.addView(btnCreatePage, params);
                    if((currentPage >= 0) && (currentActivity >= 0)){
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER), GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnEditHotspot, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(1, GridLayout.CENTER), GridLayout.spec(1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnDrawingMode, params);
                        if(currentPage >= 0 && isVideo&&
                                !prefs.getBoolean("enableVideoControlHotspots", false)){
                            if(prefs.getBoolean("useAlternateVideoLayout", true)){
                                if(pauseLocation == 2){
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, portrait ? 2 : 1, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 0, portrait ? 1 : 2, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    if(portrait){
                                        btnVideoRestart.setHeight(navLayout.getHeight());
                                        params.height = navLayout.getHeight();
                                        params.width = buttonSize;
                                    }else{
                                        btnVideoRestart.setWidth(navLayout.getWidth());
                                        params.width = navLayout.getWidth();
                                        params.height = buttonSize;
                                    }
                                    navLayout.addView(btnVideoRestart, params);
                                }else{
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, portrait ? 2 : 1, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 0, portrait ? 1 : 2, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    if(portrait){
                                        btnVideoPlay.setHeight(navLayout.getHeight());
                                        params.height = navLayout.getHeight();
                                        params.width = buttonSize;
                                    }else{
                                        btnVideoPlay.setWidth(navLayout.getWidth());
                                        params.width = navLayout.getWidth();
                                        params.height = buttonSize;
                                    }
                                    navLayout.addView(btnVideoPlay, params);
                                }
                                newDimen -= (3 * (buttonSize + (2*margin)));
                            }else{
                                if(pauseLocation == 0){
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 0, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoPlay, params);
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 1, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoStepForward, params);
                                    newDimen -= (3 * (buttonSize + (2*margin)));
                                }else if(pauseLocation == 2){
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 0, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoRestart, params);
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 1, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoStepBack, params);
                                    newDimen -= (3 * (buttonSize + (2*margin)));
                                }else{
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 0, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoRestart, params);
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 2, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 2 : 1, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoStepBack, params);
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 3, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 3 : 0, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoPlay, params);
                                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 3, GridLayout.CENTER),
                                            GridLayout.spec(portrait ? 3 : 1, GridLayout.CENTER));
                                    params.setMargins(margin, margin, margin, margin);
                                    params.height = buttonSize;
                                    params.width = buttonSize;
                                    navLayout.addView(btnVideoStepForward, params);
                                    newDimen -= (4 * (buttonSize + (2*margin)));
                                }
                            }
                        }else{
                            newDimen -= (2 * (buttonSize + (2*margin)));
                        }
                    }else{
                        newDimen -= (buttonSize + (2*margin));
                    }
                }
            }else if(currentState == State.HotspotEdit){
                if(recording){
                    params = new GridLayout.LayoutParams(GridLayout.spec(0, portrait ? 2 : 1, GridLayout.CENTER),
                            GridLayout.spec(0, portrait ? 1 : 2, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    if(portrait){
                        btnEndRecording.setHeight(navLayout.getHeight());
                        params.height = navLayout.getHeight();
                        params.width = buttonSize;
                    }else{
                        btnEndRecording.setWidth(navLayout.getWidth());
                        params.width = navLayout.getWidth();
                        params.height = buttonSize;
                    }
                    navLayout.addView(btnEndRecording, params);
                    newDimen -= (buttonSize + (2*margin));
                }else{
                    if(hotspotCreated){
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER), GridLayout.spec(0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.width = buttonSize;
                        params.height = buttonSize;
                        navLayout.addView(btnCancelHotspot, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.width = buttonSize;
                        params.height = buttonSize;
                        navLayout.addView(btnStartRecording, params);
                        newDimen -= (buttonSize + (2*margin));
                    }else if(hotspotSelected){
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER), GridLayout.spec(0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.width = buttonSize;
                        params.height = buttonSize;
                        navLayout.addView(btnCancelHotspot, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.width = buttonSize;
                        params.height = buttonSize;
                        navLayout.addView(btnDeleteHotspot, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 2, portrait ? 2 : 1, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 2 : 0, portrait ? 1 : 2, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        if(portrait){
                            btnStartRecording.setHeight(navLayout.getHeight());
                            params.height = navLayout.getHeight();
                            params.width = buttonSize;
                        }else{
                            btnStartRecording.setWidth(navLayout.getWidth());
                            params.width = navLayout.getWidth();
                            params.height = buttonSize;
                        }
                        navLayout.addView(btnStartRecording, params);
                        newDimen -= (2 * (buttonSize + (2*margin)));
                    }else{
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, portrait ? 2 : 1, GridLayout.CENTER),
                                GridLayout.spec(0, portrait ? 1 : 2, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        if(portrait){
                            btnCancelHotspot.setHeight(navLayout.getHeight());
                            params.height = navLayout.getHeight();
                            params.width = buttonSize;
                        }else{
                            btnCancelHotspot.setWidth(navLayout.getWidth());
                            params.width = navLayout.getWidth();
                            params.height = buttonSize;
                        }
                        navLayout.addView(btnCancelHotspot, params);
                        newDimen -= (buttonSize + (2*margin));
                    }
                }
            }else if((currentState == State.Drawing) || (currentState == State.Erasing)){
                if(currentMode == Mode.iSnap){
                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 2 : 0, portrait ? 2 : 1, GridLayout.CENTER),
                            GridLayout.spec(portrait ? 0 : 2, portrait ? 1 : 2, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    if(portrait){
                        btnCreatePage.setHeight(navLayout.getHeight());
                        params.height = navLayout.getHeight();
                        params.width = buttonSize;
                    }else{
                        btnCreatePage.setWidth(navLayout.getWidth());
                        params.width = navLayout.getWidth();
                        params.height = buttonSize;
                    }
                    navLayout.addView(btnCreatePage, params);
                    newDimen -= (buttonSize + (2*margin));
                }else{
                    params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 2 : 0, portrait ? 2 : 1, GridLayout.CENTER),
                            GridLayout.spec(portrait ? 0 : 2, portrait ? 1 : 2, GridLayout.CENTER));
                    params.setMargins(margin, margin, margin, margin);
                    if(portrait){
                        btnNavMode.setHeight(navLayout.getHeight());
                        params.height = navLayout.getHeight();
                        params.width = buttonSize;
                    }else{
                        btnNavMode.setWidth(navLayout.getWidth());
                        params.width = navLayout.getWidth();
                        params.height = buttonSize;
                    }
                    navLayout.addView(btnNavMode, params);
                    /*
                    boolean undoRedoAllowed = prefs.getBoolean("undo_redo_enabled", false);
                    if(dvDrawing != null){
                        if(undoRedoAllowed && dvDrawing.canRedo() && dvDrawing.canUndo()){
                            // return to nav, redo, undo
                            params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 2 : 1, GridLayout.CENTER),
                                    GridLayout.spec(portrait ? 1 : 2, GridLayout.CENTER));
                            params.setMargins(margin, margin, margin, margin);
                            params.height = buttonSize;
                            params.width = buttonSize;
                            navLayout.addView(btnUndo, params);
                            params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 3 : 1, GridLayout.CENTER),
                                    GridLayout.spec(portrait ? 1 : 3, GridLayout.CENTER));
                            params.setMargins(margin, margin, margin, margin);
                            params.height = buttonSize;
                            params.width = buttonSize;
                            navLayout.addView(btnRedo, params);
                            newDimen -= (2 * (buttonSize + (2*margin)));
                        }else if(undoRedoAllowed && dvDrawing.canRedo()){
                            // return to nav, redo
                            params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 2 : 1, portrait ? 2 : 1, GridLayout.CENTER),
                                    GridLayout.spec(portrait ? 1 : 2, portrait ? 1 : 2, GridLayout.CENTER));
                            params.setMargins(margin, margin, margin, margin);
                            if(portrait){
                                btnRedo.setHeight(navLayout.getHeight());
                                params.height = navLayout.getHeight();
                                params.width = buttonSize;
                            }else{
                                btnRedo.setWidth(navLayout.getWidth());
                                params.height = buttonSize;
                                params.width = navLayout.getWidth();
                            }
                            navLayout.addView(btnRedo, params);
                            newDimen -= (2 * (buttonSize + (2*margin)));
                        }else if(undoRedoAllowed && dvDrawing.canUndo()){
                            // return to nav, undo
                            params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 2 : 1, portrait ? 2 : 1, GridLayout.CENTER),
                                    GridLayout.spec(portrait ? 1 : 2, portrait ? 1 : 2, GridLayout.CENTER));
                            params.setMargins(margin, margin, margin, margin);
                            if(portrait){
                                btnUndo.setHeight(navLayout.getHeight());
                                params.height = navLayout.getHeight();
                                params.width = buttonSize;
                            }else{
                                btnUndo.setWidth(navLayout.getWidth());
                                params.height = buttonSize;
                                params.width = navLayout.getWidth();
                            }
                            navLayout.addView(btnUndo, params);
                            newDimen -= (2 * (buttonSize + (2*margin)));
                        }else{
                            // return to nav
                            newDimen -= (buttonSize + (2*margin));
                        }
                    }else{
                        newDimen -= (buttonSize + (2*margin));
                    }*/
                }
            }
        }else if(adminInNav && currentPage >= 0 && isVideo &&
                !prefs.getBoolean("enableVideoControlHotspots", false)){
            if(adminPanel.getChildCount() > 0){
                adminPanel.removeView(btnCreateActivity);
                adminPanel.removeView(btnCreatePage);
                adminPanel.removeView(btnEditHotspot);
                adminPanel.removeView(btnCancelHotspot);
                adminPanel.removeView(btnDeleteHotspot);
                adminPanel.removeView(btnStartRecording);
                adminPanel.removeView(btnEndRecording);
                adminPanel.removeView(btnDrawingMode);
                adminPanel.removeView(btnNavMode);
                //adminPanel.removeView(btnUndo);
                //adminPanel.removeView(btnRedo);
                adminPanel.removeView(btnVideoRestart);
                adminPanel.removeView(btnVideoStepBack);
                adminPanel.removeView(btnVideoPlay);
                adminPanel.removeView(btnVideoStepForward);
                adminPanel.removeView(btnVideoPause);
                adminPanel.setVisibility(View.GONE);
            }
            if(!videoPaused){
                params = new GridLayout.LayoutParams(GridLayout.spec(0, portrait ? 2 : 1, GridLayout.CENTER),
                        GridLayout.spec(0, portrait ? 1: 2,GridLayout.CENTER));
                params.setMargins(margin, margin, margin, margin);
                if(portrait){
                    btnVideoPause.setHeight(navLayout.getHeight());
                    params.height = navLayout.getHeight();
                    params.width = buttonSize;
                }else{
                    btnVideoPause.setWidth(navLayout.getWidth());
                    params.width = navLayout.getWidth();
                    params.height = buttonSize;
                }
                navLayout.addView(btnVideoPause, params);
                newDimen -= (1 * (buttonSize + (2*margin)));
            }else{
                if(prefs.getBoolean("useAlternateVideoLayout", true)){
                    if(pauseLocation == 2){
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, portrait ? 2 : 1, GridLayout.CENTER),
                                GridLayout.spec(0, portrait ? 1: 2,GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        if(portrait){
                            btnVideoRestart.setHeight(navLayout.getHeight());
                            params.height = navLayout.getHeight();
                            params.width = buttonSize;
                        }else{
                            btnVideoRestart.setWidth(navLayout.getWidth());
                            params.width = navLayout.getWidth();
                            params.height = buttonSize;
                        }
                        navLayout.addView(btnVideoRestart, params);
                    }else{
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, portrait ? 2 : 1, GridLayout.CENTER),
                                GridLayout.spec(0, portrait ? 1: 2,GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        if(portrait){
                            btnVideoPlay.setHeight(navLayout.getHeight());
                            params.height = navLayout.getHeight();
                            params.width = buttonSize;
                        }else{
                            btnVideoPlay.setWidth(navLayout.getWidth());
                            params.width = navLayout.getWidth();
                            params.height = buttonSize;
                        }
                        navLayout.addView(btnVideoPlay, params);
                    }
                    newDimen -= (1 * (buttonSize + (2*margin)));
                }else{
                    if(pauseLocation == 0){
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER),
                                GridLayout.spec(0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoPlay, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoStepForward, params);
                        newDimen -= (1 * (buttonSize + (2*margin)));
                    }else if(pauseLocation == 2){
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER),
                                GridLayout.spec(0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoRestart, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoStepBack, params);
                        newDimen -= (1 * (buttonSize + (2*margin)));
                    }else{
                        params = new GridLayout.LayoutParams(GridLayout.spec(0, GridLayout.CENTER),
                                GridLayout.spec(0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoRestart, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoStepBack, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(portrait ? 0 : 1, GridLayout.CENTER),
                                GridLayout.spec(portrait ? 1 : 0, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoPlay, params);
                        params = new GridLayout.LayoutParams(GridLayout.spec(1, GridLayout.CENTER),
                                GridLayout.spec(1, GridLayout.CENTER));
                        params.setMargins(margin, margin, margin, margin);
                        params.height = buttonSize;
                        params.width = buttonSize;
                        navLayout.addView(btnVideoStepForward, params);
                        newDimen -= (2 * (buttonSize + (2*margin)));
                    }
                }
            }
        }
        boolean showVideoButtons = (currentPage >= 0 && isVideo &&
                !prefs.getBoolean("enableVideoControlHotspots", false) &&
                currentState == State.Navigate);
        Log.i("setUpAdminPanel", "showVideoButtons=[" + String.valueOf(showVideoButtons) +
                "] enableVideoControlHotspots=[" + String.valueOf(!prefs.getBoolean("enableVideoControlHotspots", false)) + "]");
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        adminPanel.setVisibility((((currentMode == Mode.Create) || (currentMode == Mode.AdvancedCreate)) && !adminInNav) ? View.VISIBLE : View.INVISIBLE);
        if(currentMode == Mode.iSnap){
            btnCreateActivity.setVisibility(View.GONE);
            btnCreatePage.setVisibility(View.VISIBLE);
            btnEditHotspot.setVisibility(View.GONE);
            btnCancelHotspot.setVisibility(View.GONE);
            btnDrawingMode.setVisibility(View.GONE);
            btnNavMode.setVisibility(View.GONE);
            btnStartRecording.setVisibility(View.GONE);
            btnEndRecording.setVisibility(View.GONE);
            btnDeleteHotspot.setVisibility(View.GONE);
            //btnUndo.setVisibility(View.GONE);
            //btnRedo.setVisibility(View.GONE);
            btnVideoRestart.setVisibility(View.GONE);
            btnVideoStepBack.setVisibility(View.GONE);
            btnVideoPlay.setVisibility(View.GONE);
            btnVideoStepForward.setVisibility(View.GONE);
            btnVideoPause.setVisibility(View.GONE);
        }else{
            btnCreateActivity.setVisibility(currentState == State.Navigate ? View.VISIBLE : View.GONE);
            btnCreatePage.setVisibility(currentState == State.Navigate ? View.VISIBLE : View.GONE);
            btnEditHotspot.setVisibility(((currentState == State.Navigate) && (currentPage >= 0) && (currentActivity >= 0))
                    ? View.VISIBLE : View.GONE);
            btnCancelHotspot.setVisibility(((currentState == State.HotspotEdit) && (currentPage >= 0) && !recording)
                    ? View.VISIBLE : View.GONE);
            btnDrawingMode.setVisibility(((currentState == State.Navigate) && (currentPage >= 0) && (currentActivity >= 0))
                    ? View.VISIBLE : View.GONE);
            btnNavMode.setVisibility(((currentState == State.Drawing) || (currentState == State.Erasing)) && !recording
                    ? View.VISIBLE : View.GONE);
            btnStartRecording.setVisibility(((currentState == State.HotspotEdit) && (hotspotCreated || hotspotSelected) && !recording)
                    ? View.VISIBLE : View.GONE);
            btnEndRecording.setVisibility(((currentState == State.HotspotEdit) && (hotspotCreated || hotspotSelected) && recording)
                    ? View.VISIBLE : View.GONE);
            btnDeleteHotspot.setVisibility(((currentState == State.HotspotEdit) && hotspotSelected && !recording)
                    ? View.VISIBLE : View.GONE);
            //btnUndo.setVisibility(View.VISIBLE);
            //btnUndo.setVisibility(((currentState == State.Drawing) || (currentState == State.Erasing)) &&
            //        (dvDrawing != null) && dvDrawing.canUndo() ? View.VISIBLE : View.GONE);
            //btnRedo.setVisibility(((currentState == State.Drawing) || (currentState == State.Erasing)) &&
            //       (dvDrawing != null) && dvDrawing.canRedo() ? View.VISIBLE : View.GONE);
            btnVideoRestart.setVisibility(showVideoButtons && videoPaused && pauseLocation > 0 ? View.VISIBLE : View.GONE);
            btnVideoStepBack.setVisibility(showVideoButtons && videoPaused && pauseLocation > 0 ? View.VISIBLE : View.GONE);
            btnVideoPlay.setVisibility(showVideoButtons && videoPaused && pauseLocation < 2 ? View.VISIBLE : View.GONE);
            btnVideoStepForward.setVisibility(showVideoButtons && videoPaused && pauseLocation < 2 ? View.VISIBLE : View.GONE);
            btnVideoPause.setVisibility(showVideoButtons && !videoPaused ? View.VISIBLE : View.GONE);
        }
        if(hPages != null){
            ViewGroup.LayoutParams hpP = hPages.getLayoutParams();
            hpP.width = newDimen;
            hPages.setLayoutParams(hpP);
            hPages.requestLayout();
        }
        if(vPages != null){
            ViewGroup.LayoutParams vpP = vPages.getLayoutParams();
            vpP.height = newDimen;
            vPages.setLayoutParams(vpP);
            vPages.requestLayout();
            Log.d("vPages", "4171 vPages.width=" + vPages.getWidth());
        }
        if(hActivities != null){
            ViewGroup.LayoutParams hgP = hActivities.getLayoutParams();
            hgP.width = newDimen;
            hActivities.setLayoutParams(hgP);
            hActivities.requestLayout();
        }
        if(vActivities != null){
            ViewGroup.LayoutParams vgP = vActivities.getLayoutParams();
            vgP.height = newDimen;
            vActivities.setLayoutParams(vgP);
            vActivities.requestLayout();
        }
        GridLayout dO = (GridLayout)findViewById(R.id.DrawingOptions);
        if(dO != null){
            ViewGroup.LayoutParams dP = dO.getLayoutParams();
            if(dP != null){
                if(portrait){
                    dP.width = newDimen;
                }else{
                    dP.height = newDimen;
                }
                dO.setLayoutParams(dP);
                dO.requestLayout();
            }
        }
        navLayout.requestLayout();
    }

    private void setMode(Mode newMode, boolean save, boolean fromInit){
        Log.d("PlayTalk", "setMode: old=" + Mode.toString(currentMode) + " new=" + Mode.toString(newMode));
        if(newMode == currentMode){
            if((newMode == Mode.Navigate) || fromInit)
                return;
            currentMode =  Mode.Navigate;
        }else{
            switch(newMode){
                case Navigate:
                    currentMode = newMode;
                    break;
                case Create:
                    currentMode = newMode;
                    break;
                case AdvancedCreate:
                    currentMode = newMode;
                    break;
            }
        }
        if(fromInit){return;}
        if(currentState == State.HotspotEdit){
            currentState = State.Navigate;
            invalidateOptionsMenu();
        }
        currentState = State.Navigate;
        setDrawingOptions(currentMode == Mode.iSnap, false);
        SharedPreferences prefsEPM = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        prefsEPM.edit().putInt("currentMode", Mode.getValue(currentMode)).commit();
        prefsEPM.edit().putInt("currentState", State.getValue(currentState)).commit();
        Log.d("State", "setting state setMode");
        //if(save){saveThreadSafe();}
    }

    private void initDrawingOptions(){
        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setForegroundImageResource(MainActivity.this, R.drawable.stylus_m, true);
        ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setForegroundImageResource(MainActivity.this, R.drawable.stylus_el, true);
        ((BackgroundHighlightButton)findViewById(R.id.StylusErase)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.StylusErase)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.StylusErase)).setForegroundImageResource(MainActivity.this, R.drawable.erase_circle, true);
        ((BackgroundHighlightButton)findViewById(R.id.ClearDrawing)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ClearDrawing)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ClearDrawing)).setForegroundImageResource(MainActivity.this, R.drawable.clear_circle, true);
        ((BackgroundHighlightButton)findViewById(R.id.ClearDrawing)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlack)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlack)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlack)).setBackgroundImageResource(MainActivity.this, R.drawable.black, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setBackgroundImageResource(MainActivity.this, R.drawable.blue, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen)).setBackgroundImageResource(MainActivity.this, R.drawable.lime_green, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPink)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPink)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorPink)).setBackgroundImageResource(MainActivity.this, R.drawable.pink, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPurple)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPurple)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorPurple)).setBackgroundImageResource(MainActivity.this, R.drawable.purple, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorRed)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorRed)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorRed)).setBackgroundImageResource(MainActivity.this, R.drawable.red, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorWhite)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorWhite)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorWhite)).setBackgroundImageResource(MainActivity.this, R.drawable.white, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorYellow)).setOnClickListener(this);
        ((BackgroundHighlightButton)findViewById(R.id.ColorYellow)).setText("");
        ((BackgroundHighlightButton)findViewById(R.id.ColorYellow)).setBackgroundImageResource(MainActivity.this, R.drawable.yellow, true, true);
        ((GridLayout)findViewById(R.id.DrawingOptions)).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout() {
                Log.i("PlayTalk", "Main.initDrawingOptions.onGlobalLayout: ");
                GridLayout gl = (GridLayout)findViewById(R.id.DrawingOptions);
                if((gl != null) && (gl.getVisibility() == View.VISIBLE))
                    fillView(gl);
                int[] dimens = getImageSize();
                if(imageSizeChanged && (dvDrawing != null) && (dvDrawing.isEnabled())) {
                    //updateOverlays(currentPage, dimens);
                    if(currentDrawing != null){
                        imageSizeChanged = false;
                        dvDrawing.setDrawingInfo(getCurrentDrawing().reloadDrawing(dimens[0], dimens[1]));
                    }
                }else if(dvDrawing != null && !dvDrawing.isEnabled()) {
                    if(currentActivity >= 0) {
                        ((BackgroundHighlightButton) activityLayout.getChildAt(currentActivity)).
                                setState(BackgroundHighlightButton.State.SELECTED_ACTIVITY);
                    }if(currentPage >= 0) {
                        ((BackgroundHighlightButton) pageLayout.getChildAt(currentPage)).
                                setState(BackgroundHighlightButton.State.SELECTED_PAGE);
                    }
                }
            }
        });
    }

    private void setDrawingOptions(boolean drawing, boolean erasing){
        Log.i("State", "Main.setDrawingOptions: entry, State=" +
                "[" + String.valueOf(State.getValue(currentState)) + "]" +
                ", drawing=" + Boolean.toString(drawing) +
                ", erasing=" + Boolean.toString(erasing) );

        if(erasing)
            currentState = State.Erasing;
        else if(drawing)
            currentState = State.Drawing;
        else if(currentState != State.HotspotEdit)
            currentState = State.Navigate;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        prefs.edit().putInt("currentState", State.getValue(currentState)).commit();
        Log.d("State", "setting state setDrawingOptions state=" + State.toString(currentState, currentMode));
        dvDrawing.setEnabled(currentState == State.Drawing, currentState == State.Erasing);
        setUpAdminPanel(false, false, false);
        //Log.i("PlayTalk", "setDrawingOptions: State=[" + String.valueOf(State.getValue(currentState)) + "]");
        invalidateOptionsMenu();
        if(drawing || erasing){
            /*if(hActivities != null)
                hActivities.setVisibility(View.GONE);
            if(vActivities != null){
                vActivities.setVisibility(View.GONE);
                Log.d("vPages", "1039 vActivities Gone");
            }
            if(hPages != null)
                hPages.setVisibility(View.GONE);
            if(vPages != null){
                vPages.setVisibility(View.GONE);
                Log.d("vPages", "1045 vPages Gone");
            }*/
            GridLayout drawingOptions = (GridLayout)findViewById(R.id.DrawingOptions);
            if(drawingOptions != null)
                drawingOptions.setVisibility(View.VISIBLE);
            try{
                setActiveWidth(erasing);
                setActiveColor(erasing);
            }catch(NullPointerException e){
                e.printStackTrace();
            }
        }else{
            GridLayout drawingOptions = (GridLayout)findViewById(R.id.DrawingOptions);
            if(drawingOptions != null)
                drawingOptions.setVisibility(View.GONE);
            /*if(hActivities != null)
                hActivities.setVisibility(View.VISIBLE);
            if(vActivities != null){
                vActivities.setVisibility(View.VISIBLE);
                Log.d("vPages", "1064 vActivities Visible");
            }
            if(hPages != null)
                hPages.setVisibility(View.VISIBLE);
            if(vPages != null){
                vPages.setVisibility(View.VISIBLE);
                Log.d("vPages", "1070 vPages Visible");
            }*/
        }
        //Log.i("PlayTalk", "Main.setDrawingOptions: exit, State=[" + String.valueOf(State.getValue(currentState)) + "]" +
        //	", dvDrawing.Enabled=" + Boolean.toString(dvDrawing.isEnabled() )+
        //	", dimens=" + formatIntegerArray( getImageSize() ) );
    }

    private void setActiveWidth(boolean erasing) throws NullPointerException{
        int widthSelectedItem = (currentDrawing == null) ? 2 :
                (int)((currentDrawing.getStrokeWidth(null) - 5) / 5);
        ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);
        ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);

        if(erasing){
            ((BackgroundHighlightButton)findViewById(R.id.StylusErase)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
        }else{
            ((BackgroundHighlightButton)findViewById(R.id.StylusErase)).setState(BackgroundHighlightButton.State.NORMAL_DRAWING);
        }
        switch(widthSelectedItem){
            case 0:
            case 1:
            case 2:
                ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                break;
            case 3:
            case 4:
            case 9:
                ((BackgroundHighlightButton)findViewById(R.id.StylusEL)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                break;
            default:
                ((BackgroundHighlightButton)findViewById(R.id.StylusM)).setState(BackgroundHighlightButton.State.SELECTED_DRAWING);
                break;
        }
    }

    private void setActiveColor(boolean clear) throws NullPointerException{
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlack)).setBackgroundImageResource(MainActivity.this, R.drawable.black, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setBackgroundImageResource(MainActivity.this, R.drawable.blue, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen)).setBackgroundImageResource(MainActivity.this, R.drawable.lime_green, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPink)).setBackgroundImageResource(MainActivity.this, R.drawable.pink, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorPurple)).setBackgroundImageResource(MainActivity.this, R.drawable.purple, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorRed)).setBackgroundImageResource(MainActivity.this, R.drawable.red, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorWhite)).setBackgroundImageResource(MainActivity.this, R.drawable.white, true, true);
        ((BackgroundHighlightButton)findViewById(R.id.ColorYellow)).setBackgroundImageResource(MainActivity.this, R.drawable.yellow, true, true);
        if(clear) return;
        int colorSelectedItem = currentDrawing == null ? Color.BLUE : currentDrawing.getColor(null);

        if(colorSelectedItem == getResources().getColor(R.color.black))
            ((BackgroundHighlightButton)findViewById(R.id.ColorBlack)).setBackgroundImageResource(MainActivity.this, R.drawable.black_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.blue))
            ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setBackgroundImageResource(MainActivity.this, R.drawable.blue_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.limegreen))
            ((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen)).setBackgroundImageResource(MainActivity.this, R.drawable.lime_green_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.pink))
            ((BackgroundHighlightButton)findViewById(R.id.ColorPink)).setBackgroundImageResource(MainActivity.this, R.drawable.pink_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.purple))
            ((BackgroundHighlightButton)findViewById(R.id.ColorPurple)).setBackgroundImageResource(MainActivity.this, R.drawable.purple_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.red))
            ((BackgroundHighlightButton)findViewById(R.id.ColorRed)).setBackgroundImageResource(MainActivity.this, R.drawable.red_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.white))
            ((BackgroundHighlightButton)findViewById(R.id.ColorWhite)).setBackgroundImageResource(MainActivity.this, R.drawable.white_selected, true, true);
        else if(colorSelectedItem == getResources().getColor(R.color.yellow))
            ((BackgroundHighlightButton)findViewById(R.id.ColorYellow)).setBackgroundImageResource(MainActivity.this, R.drawable.yellow_selected, true, true);
        else
            ((BackgroundHighlightButton)findViewById(R.id.ColorBlue)).setBackgroundImageResource(MainActivity.this, R.drawable.blue_selected, true, true);
        GridLayout gl = (GridLayout)findViewById(R.id.DrawingOptions);
        fillView(gl);
    }

    public static int getColorFromIndex(int index){
        switch(index){
            case 0:
                return R.color.black;
            case 1:
                return R.color.blue;
            case 2:
                return R.color.brown;
            case 3:
                return R.color.gray;
            case 4:
                return R.color.green;
            case 5:
                return R.color.limegreen;
            case 6:
                return R.color.magenta;
            case 7:
                return R.color.pink;
            case 8:
                return R.color.purple;
            case 9:
                return R.color.red;
            case 10:
                return R.color.white;
            case 11:
                return R.color.yellow;
            default:
                return R.color.blue;
        }
    }

    public static int getIndexFromColor(int color){
        switch(color){
            case R.color.black:
                return 0;
            case R.color.blue:
                return 1;
            case R.color.brown:
                return 2;
            case R.color.gray:
                return 3;
            case R.color.green:
                return 4;
            case R.color.limegreen:
                return 5;
            case R.color.magenta:
                return 6;
            case R.color.pink:
                return 7;
            case R.color.purple:
                return 8;
            case R.color.red:
                return 9;
            case R.color.white:
                return 10;
            case R.color.yellow:
                return 11;
            default:
                return 1;
        }
    }

    public static int getIndexFromId(int id){
        switch(id){
            case R.id.ColorBlack:
                return 0;
            case R.id.ColorBlue:
                return 1;
            case R.id.ColorLimeGreen:
                return 5;
            case R.id.ColorPink:
                return 7;
            case R.id.ColorPurple:
                return 8;
            case R.id.ColorRed:
                return 9;
            case R.id.ColorWhite:
                return 10;
            case R.id.ColorYellow:
                return 11;
            default:
                return 1;
        }
    }

    private int previousChildWidth = -1;
    private int previousChildHeight = -1;
    public void fillView(GridLayout gl)
    {
        //Stretch buttons
        if(gl == null){ return; }
        int idealChildWidth = (int) (((float)gl.getWidth() - 5f)/gl.getColumnCount());
        int idealChildHeight = (int) (((float)gl.getHeight() - 5f)/gl.getRowCount());
        if((idealChildWidth == previousChildWidth) && (idealChildHeight == previousChildHeight)){return;}
        previousChildWidth = idealChildWidth;
        previousChildHeight = idealChildHeight;
        Log.d("fillView", "idealChildWidth=[" + idealChildWidth + "] width=[" + gl.getWidth() + "] columnCount=[" + gl.getColumnCount() + "]");
        Log.d("fillView", "idealChildHeight=[" + idealChildHeight + "] height=[" + gl.getHeight() + "] rowCount=[" + gl.getRowCount() + "]");
        if((idealChildWidth <= 0) || (idealChildHeight <= 0)){ return; }
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.StylusM), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.StylusEL), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.StylusErase), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ClearDrawing), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorBlack), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorBlue), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorLimeGreen), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorPink), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorPurple), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorRed), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorWhite), idealChildWidth, idealChildHeight);
        setButtonDimens((BackgroundHighlightButton)findViewById(R.id.ColorYellow), idealChildWidth, idealChildHeight);
    }

    public void setButtonDimens(BackgroundHighlightButton b, int w, int h){
        if(b != null){
            android.view.ViewGroup.LayoutParams lp = b.getLayoutParams();
            //lp.setMargins(0, 0, 0, 0);
            lp.width = w;
            lp.height = h;
            b.setLayoutParams(lp);
            b.setMinimumHeight(h);
            b.setMinimumWidth(w);
            b.setMaxHeight(h);
            b.setMaxWidth(w);

        }
    }

    int[] savedImageSize;
    boolean imageSizeChanged = true;
    public int[] getImageSize()
    {
        int[] ret = {-1,-1};
        int iW=0, iH=0;
        double rW=0.0, rH=0.0;
//		String source = "unknown";
        try{
            if( (destinationView != null) && (destinationView.getVisibility() == View.VISIBLE) ) {
                //if( (destinationView != null) ) {
                ret[0] = destinationView.getMeasuredWidth();
                ret[1] = destinationView.getMeasuredHeight();
                //Log.i("PlayTalk", "Main.getImageSize1: destinationView measuredSize=[" +
                //	formatIntegerArray(ret) );

                if(destinationView.getDrawable() != null){
                    iW = destinationView.getDrawable().getIntrinsicWidth();
                    iH = destinationView.getDrawable().getIntrinsicHeight();
                    //Log.i("PlayTalk", "Main.getImageSize2: bitmap Size=[" +
                    //		Integer.toString(iW) + "," + Integer.toString(iH) + "]");
                }
//				source = "still";
            }

            // Scale the dimensions to preserve the aspect ratio of the original image
            if((iH > 0) && (iW > 0)) {
                rW = (double)ret[0] / (double)iW;
                rH = (double)ret[1] / (double)iH;
                if(rW < rH) {
                    ret[1] = (int)Math.round(iH * rW);
                } else {
                    ret[0] = (int)Math.round(iW * rH);
                }
            }
        }catch(Exception ex){
            Log.d("PlayTalk", "Main.getImageSize Error: " + ex.getMessage( ) );
            ex.printStackTrace();
        }

        imageSizeChanged = imageSizeChanged || (savedImageSize == null) || (savedImageSize[0] != ret[0]) || (savedImageSize[1] != ret[1]) ||
                ((currentDrawing != null) && currentDrawing.requiresResize(ret[0], ret[1]));
        savedImageSize = ret;
        //Log.i("PlayTalk", "Main.getImageSize3 exit:" +
        //	" source=" + source +
        //	", ret=" + formatIntegerArray(ret) +
        //	", savedImageSize=" + formatIntegerArray(savedImageSize) +
        //	", imageSizeChanged=" + (imageSizeChanged ? "true" : "false") );
        return ret;
    }
}
