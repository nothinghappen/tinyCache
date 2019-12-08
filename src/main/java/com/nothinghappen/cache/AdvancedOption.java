package com.nothinghappen.cache;

class AdvancedOption {

    private Ticker timeWheelTicker = Tickers.SECONDS;

    private int timeWheelPower = 10;

    public AdvancedOption setTimeWheelTicker(Ticker timeWheelTicker) {
        this.timeWheelTicker = timeWheelTicker;
        return this;
    }

    public AdvancedOption setTimeWheelPower(int timeWheelPower) {
        this.timeWheelPower = timeWheelPower;
        return this;
    }

    int getTimeWheelPower() {
        return timeWheelPower;
    }

    Ticker getTimeWheelTicker() {
        return timeWheelTicker;
    }
}
