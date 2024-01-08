package org.jboss.weld.junit5.auto.beans;

import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@SuppressWarnings("serial")
public class V6 implements Engine, Serializable {

    private int throttle = 0;

    public int getThrottle() {
        return throttle;
    }

    @Override
    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

}
