package com.codeshelf.device.radio;

public class ChannelInfo {

    private int channelEnergy;
    private int controllerCount;

    // --------------------------------------------------------------------------
    /**
     * @param outControllerCount
     *            The controllerCount to set.
     */
    public void incrementControllerCount() {
        controllerCount++;
    }

    public int getChannelEnergy() {
        return channelEnergy;
    }

    public void setChannelEnergy(int channelEnergy) {
        this.channelEnergy = channelEnergy;
    }

    public int getControllerCount() {
        return controllerCount;
    }

    public void setControllerCount(int controllerCount) {
        this.controllerCount = controllerCount;
    }

}
