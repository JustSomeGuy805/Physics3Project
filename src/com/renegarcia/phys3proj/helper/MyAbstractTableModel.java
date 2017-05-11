package com.renegarcia.phys3proj.helper;
import com.renegarcia.phys3proj.BinStateInfo;
import com.renegarcia.phys3proj.PinScheme;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Rene
 */
public abstract class MyAbstractTableModel extends AbstractTableModel
{
     protected PinScheme pinScheme = new PinScheme();
     protected String[] columnNames;
     protected Class[] columnClasses;
     protected boolean[] canEdit;
     
     public MyAbstractTableModel(String[] columnNames, Class[] columnClasses, boolean[] canEdit)
     {
         this.columnNames = columnNames;
         this.columnClasses = columnClasses;
         this.canEdit = canEdit;
     }
     
     @Override
     public abstract void setValueAt(Object obj, int rowIndex, int columnIndex);
         
     
     
     @Override
     public int getRowCount()
     {
         if(pinScheme != null)
             return pinScheme.getSize();
         else 
             return 0;
     
     }
     
     
    @Override
    public String getColumnName(int col)
    {
     
        return columnNames[col];
    }
    
    
     @Override
    public Class getColumnClass(int col)
    {
        return columnClasses[col];
    }
    
    
    public void setData(PinScheme pinScheme)
    {
        PinScheme old = this.pinScheme;
       
        this.pinScheme = pinScheme;
        fireTableDataChanged();
       
        
        //if(this.elementList.size() > old.size())
            //fireTableRowsInserted(old.size(), this.elementList.size() - 1);
        //else if(this.elementList.size() < old.size())
            //fireTableRowsDeleted(this.elementList.size(), old.size() - 1);
        
       
       
        //fireTableRowsUpdated(0, elementList.size() - 1);
       
    }
    
    
    public PinScheme getData()
    {
        return pinScheme;
    }
    
    public BinStateInfo getDataAt(int row)
    {
        if(pinScheme != null && pinScheme.getSize() > 0)
            return pinScheme.get(row);
        else 
            return null;
    }
    
    
    @Override
    public int getColumnCount()
    {
       return columnNames.length;
    }

    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return canEdit[columnIndex];
        
    }
    
    
    public void removeRow(int rowindex)
    {
        pinScheme.remove(rowindex);
        fireTableRowsDeleted(rowindex, rowindex);
    }
    
     
}
