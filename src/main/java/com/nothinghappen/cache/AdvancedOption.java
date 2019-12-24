package com.nothinghappen.cache;

class AdvancedOption {

    private Ticker timeWheelTicker = Tickers.SECONDS;

    private int timeWheelPower = 10;

    private Ticker cacheTicker = Tickers.NANO;

    public AdvancedOption setTimeWheelTicker(Ticker timeWheelTicker) {
        this.timeWheelTicker = timeWheelTicker;
        return this;
    }

    public AdvancedOption setTimeWheelPower(int timeWheelPower) {
        this.timeWheelPower = timeWheelPower;
        return this;
    }

    public AdvancedOption setCacheTicker(Ticker cacheTicker) {
        this.cacheTicker = cacheTicker;
        return this;
    }

    int getTimeWheelPower() {
        return timeWheelPower;
    }

    Ticker getTimeWheelTicker() {
        return timeWheelTicker;
    }

    Ticker getCacheTicker() {
        return cacheTicker;
    }
}
