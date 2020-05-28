package io.github.alopukhov.dare.clg.spi;

import lombok.ToString;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@ToString
public class SimpleUrlHolder implements UrlHolder {
    private final List<URL> urls;

    private SimpleUrlHolder(List<URL> urls) {
        this.urls = requireNonNull(urls);
        for (URL url : urls) {
            requireNonNull(url);
        }
    }

    public static SimpleUrlHolder create(URL url) {
        return new SimpleUrlHolder(singletonList(url));
    }

    public static SimpleUrlHolder create(Collection<URL> urls) {
        return new SimpleUrlHolder(new ArrayList<>(urls));
    }

    @Override
    public Collection<URL> getURLs() {
        return urls;
    }
}
