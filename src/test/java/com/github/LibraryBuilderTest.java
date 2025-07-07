package com.github;

import com.github.adamorgan.api.LibraryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public class LibraryBuilderTest extends AbstractSnapshotTest
{
    private static final String TOKEN = "invalid.token.here";

    static class TestLibraryBuilder extends LibraryBuilder
    {
        public TestLibraryBuilder(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
        {
            super(address, username, password);
        }

        @Override
        public LibraryBuilder applyDefault()
        {
            return super.applyDefault();
        }

        @Override
        public LibraryBuilder applyLight()
        {
            return super.applyLight();
        }
    }
}
