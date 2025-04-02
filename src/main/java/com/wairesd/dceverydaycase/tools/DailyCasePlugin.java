package com.wairesd.dceverydaycase.tools;

import com.jodexindustries.donatecase.api.DCAPI;
import com.wairesd.dceverydaycase.service.DailyCaseService;

/**
 * Интерфейс для доступа к основным компонентам плагина.
 */
public interface DailyCasePlugin {
    DCAPI getDCAPI();
    DailyCaseService getDailyCaseService();
}
