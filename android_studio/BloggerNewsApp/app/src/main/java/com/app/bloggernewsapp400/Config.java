package com.app.bloggernewsapp400;

import com.app.bloggernewsapp400.util.Constant;

public class Config {

    // Optional Google Drive file ID or direct JSON URL.
    // Keep this empty until the real config source is added.
    // The app will use its last successfully downloaded configuration when offline.
    public static final String REMOTE_CONFIG_SOURCE = "14NDMQqy10bQvUAmu5eocVEpgNdQNmmt1";

    //app UI design
    public static final boolean ENABLE_NEW_APP_DESIGN = true;

    //if set false, the app will use settings icon from drawable-hdpi folder
    public static final boolean SET_LAUNCHER_IMAGE_AS_HOME_TOP_RIGHT_ICON = true;

    //"published": Order by the date the post was published
    //"updated": Order by the date the post was last updated
    public static final String DISPLAY_POST_ORDER = "published";

    //if it's true the first image in the post details will be the main image
    public static final boolean FIRST_POST_IMAGE_AS_MAIN_IMAGE = true;

    //label sorting, supported value : Constant.LABEL_NAME_ASCENDING, Constant.LABEL_NAME_DESCENDING or Constant.LABEL_DEFAULT
    public static final String LABELS_SORTING = Constant.LABEL_NAME_ASCENDING;

    //category layout style, supported value : Constant.CATEGORY_LIST, Constant.CATEGORY_GRID_SMALL or Constant..CATEGORY_GRID_MEDIUM
    public static final String CATEGORY_LAYOUT_STYLE = Constant.CATEGORY_GRID_SMALL;

    //category image style, supported value : Constant.CIRCULAR or Constant.ROUNDED
    public static final String CATEGORY_IMAGE_STYLE = Constant.CIRCULAR;

    //enable copy text in the story content
    public static final boolean ENABLE_TEXT_SELECTION = false;

    //enable GDPR EU consent
    public static final boolean ENABLE_GDPR_UMP_SDK = true;

    //delay splash when remote config finish loading in millisecond
    public static final int DELAY_SPLASH = 500;

    //Enable it with true value if want to the app will force to display open ads first before start the main menu
    //Longer duration to start the app may occur depending on internet connection or open ad response time itself
    public static final boolean FORCE_TO_SHOW_APP_OPEN_AD_ON_START = false;

}