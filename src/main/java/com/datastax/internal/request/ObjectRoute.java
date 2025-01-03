package com.datastax.internal.request;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.stream.Stream;

public class ObjectRoute
{
	private final String path;

	public ObjectRoute(String path)
    {
		this.path = path;
	}

    public CompileRoute compile(String... params)
    {
		String compileRoute = Stream.of(params).reduce(path, this::replace);

		System.out.println(compileRoute);

		return new CompileRoute(this, compileRoute);
    }

	private String replace(String path, String param)
	{
		String replacement = NumberUtils.isDigits(param) ? param : "'" + param + "'";
		return StringUtils.replaceFirst(path, "\\?", replacement);
	}

    public class CompileRoute
    {
		private final ObjectRoute route;
		private final String compileRoute;

		public CompileRoute(ObjectRoute route, String compileRoute)
        {
			this.route = route;
			this.compileRoute = compileRoute;
		}

		public String getCompiledRoute() {
			return this.compileRoute;
		}
	}
}
