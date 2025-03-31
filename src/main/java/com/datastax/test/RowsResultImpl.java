package com.datastax.test;

import com.datastax.api.entities.Column;
import com.datastax.api.requests.ObjectAction;
import com.datastax.internal.entities.ColumnImpl;
import com.datastax.internal.entities.RowImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class RowsResultImpl implements ObjectAction
{
    public static final String TEST_QUERY = "SELECT * FROM system.clients";

    private final ByteBuf buffer;
    private final LinkedList<ColumnImpl> columns = new LinkedList<>();
    private final LinkedList<RowImpl> rows = new LinkedList<>();

    public RowsResultImpl(@Nonnull final ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    public void run()
    {
        int flags = buffer.readInt();

        boolean hasGlobalTableSpec = (flags & 0x0001) != 0;
        boolean hasMorePages = (flags & 0x0002) != 0;
        boolean noMetadata = (flags & 0x0004) != 0;

        int columnsCount = buffer.readInt();

        if (!noMetadata)
        {
            for (int i = 0; i < columnsCount; i++)
            {
                columns.add(new ColumnImpl(buffer));
            }
        }

        int rowsCount = buffer.readInt();
        System.out.println("Rows count: " + rowsCount);

        for (int rowNumber = 0; rowNumber < rowsCount; rowNumber++)
        {
            for (ColumnImpl column : columns)
            {
                RowImpl row = new RowImpl(column, buffer);
                this.rows.addLast(row);
            }
        }

        LinkedList<String> columns = this.columns.stream().map(ColumnImpl::toString).collect(Collectors.toCollection(LinkedList::new));
        LinkedList<String> rows = this.rows.stream().map(RowImpl::toString).collect(Collectors.toCollection(LinkedList::new));

        StringUtils.Table table = new StringUtils.Table(columns, rows);
        System.out.println(table);
    }
}
