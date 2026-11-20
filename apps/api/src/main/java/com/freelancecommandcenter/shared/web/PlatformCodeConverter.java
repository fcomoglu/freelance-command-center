package com.freelancecommandcenter.shared.web;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Reads platform codes in query and path parameters in their API form ({@code freelancer}, {@code arc_dev}). */
@Component
class PlatformCodeConverter implements Converter<String, PlatformCode> {

    @Override
    public PlatformCode convert(String source) {
        return PlatformCode.fromValue(source.trim());
    }
}
