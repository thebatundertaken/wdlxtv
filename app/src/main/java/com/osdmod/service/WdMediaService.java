package com.osdmod.service;

import android.util.Log;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class WdMediaService {
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
    private final UpnpServiceConnection serviceConnection;
    /**
     * @noinspection rawtypes
     */
    private final Service renderingControl;
    /**
     * @noinspection rawtypes
     */
    private final Service avTransportControl;
    private final WdMediaServiceCallback mediaDeviceCallback;
    private final ThreadPoolExecutor executor;
    private int volumen = 100;
    private String playMode = PLAY_MODE_NORMAL;

    public WdMediaService(UpnpServiceConnection serviceConnection,
                          WdMediaServiceCallback mediaDeviceCallback) {
        this.serviceConnection = serviceConnection;
        this.mediaDeviceCallback = mediaDeviceCallback;

        renderingControl = serviceConnection.getRemoteDevice()
                .findService(new UDAServiceType(RENDERING_CONTROL_SERVICE));
        renderingControl.getActions();

        avTransportControl = serviceConnection.getRemoteDevice()
                .findService(new UDAServiceType(AV_TRANSPORT_SERVICE));
        avTransportControl.getActions();

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }

    public int getVolumen() {
        return volumen;
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
                volumen = finalNewValue;
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
            }
        });

    }

    public void syncMediaInfo() {
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation<>(
                avTransportControl.getAction(GET_MEDIA_INFO_ACTION));
        invocation.setInput("InstanceID", "0");
        serviceConnection.getUpnpService().getControlPoint()
                .execute(new ActionCallback(invocation) {
                    public void success(ActionInvocation invocation) {
                        String title = getMediaTitleFromMetadata(
                                invocation.getOutput("CurrentURIMetaData").getValue().toString());
                        mediaDeviceCallback.onMediaTitleReceived(title);
                    }

                    public void failure(ActionInvocation invocation, UpnpResponse operation,
                                        String defaultMsg) {
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
                volumen = Integer.parseInt(
                        invocation.getOutput("CurrentVolume").getValue().toString());

                mediaDeviceCallback.onVolumenChanged(volumen);
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
            }
        });
    }

    public void circlePlayMode() {
        switch (playMode) {
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
        return playMode;
    }

    private void setPlayMode(String newMode) {
        if (avTransportControl.getAction(SET_PLAY_MODE_ACTION) == null) {
            return;
        }

        playMode = newMode;
        //noinspection rawtypes,unchecked
        ActionInvocation<?> invocation = new ActionInvocation(
                avTransportControl.getAction(SET_PLAY_MODE_ACTION));
        invocation.setInput("InstanceID", "0");
        invocation.setInput("NewPlayMode", playMode);
        performInvocation(new ActionCallback(invocation) {
            public void success(ActionInvocation invocation) {
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
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
        serviceConnection.getUpnpService().getControlPoint()
                .execute(new ActionCallback(setTargetInvocation) {
                    public void success(ActionInvocation invocation) {
                    }

                    public void failure(ActionInvocation invocation, UpnpResponse operation,
                                        String defaultMsg) {
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
                if (invocation.getOutput("TrackDuration")
                        .getValue() == null || invocation.getOutput("RelTime")
                        .getValue() == null) {
                    Log.i(TAG, "ERROR obtaining playback position");
                    return;
                }
                String trackDuration = invocation.getOutput("TrackDuration").getValue()
                        .toString();
                String relTime = invocation.getOutput("RelTime").getValue().toString();

                mediaDeviceCallback.onPlaybackPositionChanged(trackDuration, relTime);
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
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
                playMode = invocation.getOutput("PlayMode").getValue().toString();
                mediaDeviceCallback.onPlaymodeChanged(playMode);
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
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
                String playbackStatus = invocation.getOutput("CurrentTransportState").getValue()
                        .toString();
                mediaDeviceCallback.onPlaybackStatusChanged(playbackStatus);
            }

            public void failure(ActionInvocation invocation, UpnpResponse operation,
                                String defaultMsg) {
                Log.e(TAG, defaultMsg);
            }
        });
    }

    public void volumenUp() {
        if (volumen == VOLUMEN_MAX) {
            return;
        }

        volumen += VOLUMEN_INCREMENT;
        if (volumen > VOLUMEN_MAX) {
            volumen = VOLUMEN_MAX;
        }
        setVolumen(volumen);
    }

    public void volumenDown() {
        if (volumen == VOLUMEN_MIN) {
            return;
        }

        volumen -= VOLUMEN_INCREMENT;
        if (volumen < VOLUMEN_MIN) {
            volumen = VOLUMEN_MIN;
        }
        setVolumen(volumen);
    }

    private void performInvocation(ActionCallback callback) {
        executor.submit(() -> {
                    serviceConnection.getUpnpService().getControlPoint().execute(callback);
                }
        );
    }

    private String getMediaTitleFromMetadata(String XML) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(XML.replaceAll("&", "&amp;")));
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                if (eventType != 2) {
                    continue;
                }

                if (xpp.getAttributeCount() == 0 && xpp.getName().equals("title")) {
                    return xpp.nextText();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return null;
    }

}

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
