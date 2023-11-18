package org.zagvladimir.model;

import java.time.LocalDate;

public record Currency(
        int Cur_ID,
        LocalDate Date,
        String Cur_Abbreviation,
        int Cur_Scale,
        String Cur_Name,
        double Cur_OfficialRate
) {}
