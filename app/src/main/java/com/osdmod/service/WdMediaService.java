package com.osdmod.service;

import android.util.Log;

import com.osdmod.service.listener.WdMediaServiceEventListener;
import com.osdmod.service.listener.WdUpnpSubscriptionEventListener;
import com.osdmod.utils.XmlUtils;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.UDAServiceType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WdMediaService implements WdUpnpSubscriptionEventListener {
    public static final String PLAY_MODE_REPEAT_ONE = "REPEAT_ONE";
    public static final String PLAY_MODE_REPEAT_ALL = "REPEAT_ALL";
    public static final String PLAY_MODE_RANDOM = "RANDOM";
    public static final String PLAY_MODE_NORMAL = "NORMAL";
    public static final String PLAYBACK_PAUSED_PLAYBACK = "PAUSED_PLAYBACK";
    public static final String PLAYBACK_PREBUFFING = "PREBUFFING";
    public static final String PLAYBACK_STOPPED = "STOPPED";
    public static final String PLAYBACK_PLAYING = "PLAYING";
    public static final String PLAYBACK_TRANSITIONING = "TRANSITIONING";
    public static final String PLAYBACK_NO_MEDIA_PRESENT = "NO_MEDIA_PRESENT";
    private static final String TAG = "WdMediaService";
    private static final String RENDERING_CONTROL_SERVICE = "RenderingControl";
    private static final String AV_TRANSPORT_SERVICE = "AVTransport";
    private static final String SET_PLAY_MODE_ACTION = "SetPlayMode";
    private static final String SET_VOLUMEN_ACTION = "SetVolume";
    private static final String GET_VOLUMEN_ACTION = "GetVolume";
    private static final String GET_TRANSPORT_SETTINGS_ACTION = "GetTransportSettings";
    private static final String GET_TRANSPORT_INFO_ACTION = "GetTransportInfo";
    private static final String GET_POSITION_INFO_ACTION = "GetPositionInfo";
    private static final String GET_MEDIA_INFO_ACTION = "GetMediaInfo";
    private static final String SEEK_ACTION = "Seek";
    private static final int VOLUMEN_INCREMENT = 10;
    private static final int VOLUMEN_MAX = 100;
    private static final int VOLUMEN_MIN = 0;
    private static final int MAX_TOTAL_RETRIES = 5;
    private final Service<?,?> renderingControl;
    private final Service<?,?> avTransportControl;
    private final RemoteDevice remoteDevice;
    private final ControlPoint controlPoint;
    private final WdMediaServiceEventListener mediaEventListener;
    private final ExecutorService executor;
    private int mediaVolumen = 100;
    private int mediaPlaySpeed = 1;
    private String mediaPlayMode = PLAY_MODE_NORMAL;
    private String mediaPlaybackState = WdMediaService.PLAYBACK_STOPPED;
    private int retries = 0;
    private boolean subscribeToDevice = false;

    public WdMediaService(RemoteDevice remoteDevice, ControlPoint controlPoint,
                          WdMediaServiceEventListener mediaEventListener) {
        this.remoteDevice = remoteDevice;
        this.controlPoint = controlPoint;
        this.mediaEventListener = mediaEventListener;

        renderingControl = remoteDevice.findService(new UDAServiceType(RENDERING_CONTROL_SERVICE));
        renderingControl.getActions();

        avTransportControl = remoteDevice.findService(new UDAServiceType(AV_TRANSPORT_SERVICE));
        avTransportControl.getActions();

        executor = Executors.newFixedThreadPool(2);
        //executor = Executors.newSingleThreadExecutor();
    }

    public void subscribeToRemoteDevice() {
        subscribeToDevice = true;
        SubscriptionCallback callback = new SubscriptionCallbackWrapper(remoteDevice.getServices()[0], this);
        controlPoint.execute(callback);
    }

    /** @noinspection unused*/
    public void unsubscribeFromRemoteDevice() {
        subscribeToDevice = false;
    }

    public String getPlaybackState() {
        return mediaPlaybackState;
    }

    public void initialSync() {
        syncPlaybackStatus();
        syncPlaybackPosition();
        syncVolumen();
        syncPlayMode();
    }

    public int getVolumen() {
        return mediaVolumen;
    }

    /** @noinspection unused*/
    public int getMediaPlaySpeed() {
        return mediaPlaySpeed;
    }

    /**
     * @param newValue 0 to 100
     */
    public void setVolumen(int newValue) {
        if (newValue > VOLUMEN_MAX) {
            newValue = VOLUMEN_MAX;
        } else if (newValue < VOLUMEN_MIN) {
            newValue = VOLUMEN_MIN;
        }

        if (renderingControl.getAction(SET_VOLUMEN_ACTION) == null) {
            return;
        }

        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                renderingControl.getAction(SET_VOLUMEN_ACTION));
        invocation.setInput("InstanceID", "0");
        invocation.setInput("Channel", "Master");
        invocation.setInput("DesiredVolume", Integer.toString(newValue));
        int finalNewValue = newValue;

        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                mediaVolumen = finalNewValue;
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    setVolumen(finalNewValue);
                }
            }
        });

    }

    public void syncVolumen() {
        if (renderingControl.getAction(GET_VOLUMEN_ACTION) == null) {
            return;
        }

        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                renderingControl.getAction(GET_VOLUMEN_ACTION));
        invocation.setInput("InstanceID", "0");
        invocation.setInput("Channel", "Master");

        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                try {
                    mediaVolumen = Integer.parseInt(
                            invocation.getOutput("CurrentVolume").getValue().toString());

                    mediaEventListener.onVolumenChanged(mediaVolumen);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    syncVolumen();
                }
            }
        });
    }

    public void circlePlayMode() {
        switch (mediaPlayMode) {
            case PLAY_MODE_NORMAL:
                setPlayMode(PLAY_MODE_REPEAT_ONE);
                break;

            case PLAY_MODE_REPEAT_ONE:
                setPlayMode(PLAY_MODE_REPEAT_ALL);
                break;

            case PLAY_MODE_REPEAT_ALL:
                setPlayMode(PLAY_MODE_RANDOM);
                break;

            default:
                setPlayMode(PLAY_MODE_NORMAL);
                break;
        }
    }

    public String getPlayMode() {
        return mediaPlayMode;
    }

    private void setPlayMode(String newMode) {
        if (avTransportControl.getAction(SET_PLAY_MODE_ACTION) == null) {
            return;
        }

        mediaPlayMode = newMode;
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                avTransportControl.getAction(SET_PLAY_MODE_ACTION));
        invocation.setInput("InstanceID", "0");
        invocation.setInput("NewPlayMode", mediaPlayMode);
        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    setPlayMode(newMode);
                }
            }
        });
    }

    public void setPlaybackPosition(String position) {
        if (avTransportControl.getAction(SEEK_ACTION) == null) {
            return;
        }

        //noinspection rawtypes,unchecked
        ActionInvocation<?> setTargetInvocation = new ActionInvocation(
                avTransportControl.getAction(SEEK_ACTION));
        setTargetInvocation.setInput("InstanceID", "0");
        setTargetInvocation.setInput("Unit", "REL_TIME");
        setTargetInvocation.setInput("Target", position);
        controlPoint.execute(new ActionCallback(setTargetInvocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    setPlaybackPosition(position);
                }
            }
        });

    }

    public void syncPlaybackPosition() {
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                avTransportControl.getAction(GET_POSITION_INFO_ACTION));
        invocation.setInput("InstanceID", "0");

        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                if (invocation.getOutput("TrackDuration")
                        .getValue() == null || invocation.getOutput("RelTime")
                        .getValue() == null) {
                    Log.i(TAG, "ERROR obtaining playback position");
                    return;
                }
                try {
                    String trackDuration = invocation.getOutput("TrackDuration").getValue()
                            .toString();
                    String relTime = invocation.getOutput("RelTime").getValue().toString();
                    mediaEventListener.onPlaybackPositionChanged(trackDuration, relTime);
                } catch (Exception ignored) {
                }

                try {
                    String title = getMediaTitleFromMetadata(
                            invocation.getOutput("TrackMetaData").getValue().toString());
                    mediaEventListener.onMediaTitleReceived(title);
                } catch (Exception ignored) {
                    mediaEventListener.onMediaTitleReceived("");
                }
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    syncPlaybackPosition();
                }
            }
        });
    }

    public void syncPlayMode() {
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                avTransportControl.getAction(GET_TRANSPORT_SETTINGS_ACTION));
        invocation.setInput("InstanceID", "0");

        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                try {
                    mediaPlayMode = invocation.getOutput("PlayMode").getValue().toString();
                    mediaEventListener.onPlaymodeChanged(mediaPlayMode);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    syncPlayMode();
                }
            }
        });
    }

    public void syncPlaybackStatus() {
        //noinspection rawtypes,unchecked
        ActionInvocation<?> setTargetInvocation = new ActionInvocation(
                avTransportControl.getAction(GET_TRANSPORT_INFO_ACTION));
        setTargetInvocation.setInput("InstanceID", "0");
        performInvocation(new ActionCallback(setTargetInvocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                try {
                    String newPlaybackState = invocation.getOutput("CurrentTransportState")
                            .getValue()
                            .toString();
                    Log.i(TAG,
                            "syncPlaybackStatus:: prev state=" + mediaPlaybackState + "; new state=" + newPlaybackState);
                    mediaPlaybackState = newPlaybackState;
                    mediaEventListener.onPlaybackStatusChanged(mediaPlaybackState);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    syncPlaybackStatus();
                }
            }
        });
    }

    public void volumenUp() {
        if (mediaVolumen == VOLUMEN_MAX) {
            return;
        }

        mediaVolumen += VOLUMEN_INCREMENT;
        if (mediaVolumen > VOLUMEN_MAX) {
            mediaVolumen = VOLUMEN_MAX;
        }
        setVolumen(mediaVolumen);
    }

    public void volumenDown() {
        if (mediaVolumen == VOLUMEN_MIN) {
            return;
        }

        mediaVolumen -= VOLUMEN_INCREMENT;
        if (mediaVolumen < VOLUMEN_MIN) {
            mediaVolumen = VOLUMEN_MIN;
        }
        setVolumen(mediaVolumen);
    }

    private void performInvocation(ActionCallback callback) {
        executor.submit(() -> {
                    controlPoint.execute(callback);
                }
        );
    }

    private String getMediaTitleFromMetadata(String XML) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(XmlUtils.escape(XML)));
            int eventType = xpp.getEventType();
            while (eventType != 1) {
                try {
                    eventType = xpp.next();
                    if (eventType != 2) {
                        continue;
                    }

                    if (xpp.getAttributeCount() == 0 && xpp.getName().equals("title")) {
                        return xpp.nextText();
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }

        return null;
    }

    private void newEvent(String XML) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(XmlUtils.escape(XML)));
            int eventType = xpp.getEventType();
            while (eventType != 1) {
                eventType = xpp.next();
                if (eventType != 2) {
                    continue;
                }

                switch (xpp.getName()) {
                    case "TransportState":
                    //case "TransportStatus":
                        String state = xpp.getAttributeValue(null, "val");
                        mediaEventListener.onPlaybackStatusChanged(state);
                        break;

                    case "CurrentTrackMetaData":
                        try {
                            String title = xpp.getAttributeValue(null, "val");
                            title = getMediaTitleFromMetadata(XmlUtils.unescape(title));
                            mediaEventListener.onMediaTitleReceived(title);
                        } catch (Exception ignored) {
                            mediaEventListener.onMediaTitleReceived(null);
                        }
                        break;

                    case "TransportPlaySpeed":
                        mediaPlaySpeed = Integer.parseInt(xpp.getAttributeValue(null, "val"));
                        mediaEventListener.onPlaybackSpeedChanged(mediaPlaySpeed);
                        break;

                    case "CurrentPlayMode":
                        mediaPlayMode = xpp.getAttributeValue(null, "val");
                        mediaEventListener.onPlaymodeChanged(mediaPlayMode);
                        break;

                    default:
                        Log.d(TAG, "XML field " + xpp.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onServiceSubscribed() {
        initialSync();
    }

    @Override
    public void onServiceSubscriptionFailed(Exception e) {
        mediaEventListener.onFail(e);
    }

    @Override
    public void onServiceSubscriptionEnded(CancelReason cancelReason) {
        //RENEWAL_FAILED
        //DEVICE_WAS_REMOVED
        if(subscribeToDevice) {
            subscribeToRemoteDevice();
        }
    }

    /** @noinspection rawtypes*/
    @Override
    public void onSubscriptionEventReceived(GENASubscription genaSubscription) {
        if (genaSubscription.getCurrentSequence().getValue() == 0) {
            return;
        }

        //noinspection unchecked
        Map<String, StateVariableValue<?>> values = genaSubscription.getCurrentValues();
        String XML = Objects.requireNonNull(values.get("LastChange")).getValue().toString();
        newEvent(XML);
    }

    @Override
    public void onSubscriptionEventMissed() {
    }

}

    /*public void syncMediaInfo() {
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation<>(
                avTransportControl.getAction(GET_MEDIA_INFO_ACTION));
        invocation.setInput("InstanceID", "0");
        controlPoint.execute(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
                retries = 0;
                try {
                    String title = getMediaTitleFromMetadata(
                            invocation.getOutput("CurrentURIMetaData").getValue()
                                    .toString());
                    mediaEventListener.onMediaTitleReceived(title);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.d(TAG, defaultMsg);
                if (retries++ < MAX_TOTAL_RETRIES) {
                    syncMediaInfo();
                }
            }
        });
    }*/

    /*private void listPres() {
        ActionInvocation setTargetInvocation = new ActionInvocation(
                remoteDevice.findService(new UDAServiceType("RenderingControl"))
                        .getAction("ListPresets"));
        setTargetInvocation.setInput("InstanceID", "0");
        upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation) {
            public void success(ActionInvocation invocation) {
                //ActionArgumentValue[] output = invocation.getOutput();
                MainActivity.current_pres = invocation.getOutput("CurrentPresetNameList")
                        .getValue().toString();
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                System.err.println(defaultMsg);
            }
        });
    }*/

    /*private boolean getMute(Service service) {
        ActionInvocation setTargetInvocation = new ActionInvocation(service.getAction("GetMute"));
        setTargetInvocation.setInput("InstanceID", "0");
        setTargetInvocation.setInput("Channel", "Master");
        upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation) {
            public void success(ActionInvocation invocation) {
                //ActionArgumentValue[] output = invocation.getOutput();
                MainActivity.current_mute = (Boolean) invocation.getOutput("CurrentMute")
                        .getValue();
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                System.err.println(defaultMsg);
            }
        });
        return current_mute;
    }*/
