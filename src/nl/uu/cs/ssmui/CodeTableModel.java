package nl.uu.cs.ssmui;

import java.util.Vector;

import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import nl.uu.cs.ssm.Instruction;
import nl.uu.cs.ssm.MachineState;
import nl.uu.cs.ssm.Memory;
import nl.uu.cs.ssm.MemoryCellEvent;
import nl.uu.cs.ssm.MemoryCellListener;
import nl.uu.cs.ssm.MetaInstruction;
import nl.uu.cs.ssm.Registers;
import nl.uu.cs.ssm.Utils;

public class CodeTableModel extends AbstractTableModel 
	implements MemoryCellListener, CodeRowSupplier
{
	private static final long serialVersionUID = 1L ;

	protected static final int C_LABEL 	    = 0 ;
	protected static final int C_ADDRESS 	= 1 ;
	protected static final int C_PC 		= 2 ;
	protected static final int C_BP 		= 3 ;
	protected static final int C_VALUE 	    = 4 ;
	protected static final int C_INSTR 	    = 5 ;
	protected static final int C_ARG1 	    = 6 ;
	protected static final int C_ARG2 	    = 7 ;
	
    private static final String[] columnNames = { "Label", "Address", "PC", "BP", "Value", "Instr", "Arg1", "Arg2" } ;
    
    private SSMRunner			ssmRunner ;
    private MachineState		machineState ;
    private Memory 				memory ;
    private Registers 			registers ;
    private Labels   			labels ;
    
    private int                 lastPCRow ;
    
    private BoundedRangeModel 	verticalScrollBarModel ;
	
	private class Row
    {
        int         memLoc ;
        Instruction cachedInstr ;
        boolean     hasBreakPoint = false ;
        String      labelName ;
        String		usedLabelName ;
        Vector<MetaInstruction>      metaInstructions ;
        
        Row( Instruction i, int loc, String l )
        {
            cachedInstr = i ;
            memLoc = loc ;
            memory.reserveAt( loc, cachedInstr.getNrMemCells() ) ;
            memory.setAt( loc, cachedInstr.getCode() ) ;
            labelName = l ;
            usedLabelName = null ;
            metaInstructions = null ;
        }
        
        Row( String i, int loc, String l )
        {
            this( Instruction.findByRepr( i ), loc, l ) ;
        }
        
        String getInstrRepr( int memLoc )
        {
            //System.out.println( "row mem at " + memLoc ) ;
            if ( usedLabelName == null )
	            return cachedInstr.getRepr( memory.getAt( memLoc+1, cachedInstr.getNrInlineOpnds() ) ) ;
	        else
	            return cachedInstr.getRepr( usedLabelName ) ;
        }
        
        boolean replaceInstr( int row, Instruction instr )
        {
        	int shiftUp = instr.getNrMemCells() - getNrMemCells() ;
        	memory.shiftAt( memLoc, shiftUp ) ;
        	shiftMemLocations( row+1, shiftUp ) ;
        	memory.setAt( memLoc, instr.getCode() ) ;
        	cachedInstr = instr ;
        	return shiftUp != 0 ;
        }
        
        int getNrMemCells()
        {
            return cachedInstr.getNrMemCells() ;
        }
        
        int getNrInlineOpnds()
        {
            return cachedInstr.getNrInlineOpnds() ;
        }
        
        void addMetaInstruction( MetaInstruction mi )
        {
            if ( metaInstructions == null )
                metaInstructions = new Vector<MetaInstruction>() ;
            if ( mi != null )
                metaInstructions.addElement( mi ) ;
        }
        
        void setUsedLabel( String l )
        {
        	usedLabelName = l ;
        }
        
    }
    
    private Vector<Row> rows ;
    
    private Row getRowAt( int row )
    {
        return rows.elementAt( row ) ;
    }

    private boolean isValidRow( int row )
    {
        return row >= 0 && row < getRowCount() ;
    }

    private int memLocOfRow( int row, boolean before )
    {
        Row r = getRowAt( row ) ;
        return r.memLoc + ( before ? 0 : r.getNrMemCells() ) ;
    }

    public int memLocOfRow( int row )
    {
        return memLocOfRow( row, true ) ;
    }

    private int rowOfMemLoc( int loc )
    {
        int row ;
        int max = getRowCount() ;
    	for ( row = 0 ; row < max ; row++ )
    	{
    		Row r = getRowAt( row ) ;
    		if ( ( loc >= r.memLoc ) && ( loc < ( r.memLoc + r.getNrMemCells() ) ) )
    			return row ;
    	}
        return -1 ;
    }
    
    protected boolean hasBreakpointAtPC()
    {
        boolean res = false ;
        int memLoc = registers.getReg( Registers.PC ) ;
        int row = rowOfMemLoc( memLoc ) ;
        if ( isValidRow( row ) )
        {
            Row r = getRowAt( row ) ;
            res = r.hasBreakPoint ;
        }
        return res ;
    }

	private void shiftMemLocations( int row, int shift )
	{
		if ( shift != 0 )
		{
			int max = getRowCount() ;
			for ( int i = row ; i < max ; i++ )
			{
				Row r = getRowAt( i ) ;
				r.memLoc += shift ;
			}
		}
	}
	
    public void insertNewInstrAt( int row, boolean doBefore )
    {
        Row r = new Row( "nop", memLocOfRow( row, doBefore ), null ) ;
        int insertRow = row + (doBefore ? 0 : 1) ;
        shiftMemLocations( insertRow, r.getNrMemCells() ) ;
        rows.insertElementAt( r, insertRow ) ;
        fireTableRowsInserted( insertRow, insertRow ) ;
        //fireTableChanged( new TableModelEvent( this, insertRow, insertRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT) ) ;
        fireTableRowsUpdated( insertRow+1, getRowCount()-1 ) ;
        //fireTableChanged( new TableModelEvent( this, insertRow+1, getRowCount()-1 ) ) ;
    }

    public void beforeReset()
    {
        if ( memory != null )
        	memory.removeMemoryCellListener( this ) ;
        if ( registers != null )
	        registers.removeMemoryCellListener( this ) ;
    }
    
    public void reset()
    {
        labels.reset( Registers.getRegNAliasNames() ) ;
        rows = new Vector<Row>() ;
        memory = machineState.getMemory() ;
        registers = machineState.getRegisters() ;
        memory.addMemoryCellListener( this ) ;
        registers.addMemoryCellListener( this ) ;
        rows.addElement( new Row( "halt", 0, null ) ) ;
        lastPCRow = 0 ;
        fireTableChanged( new TableModelEvent( this ) ) ;
        //fireTableStructureChanged() ;
    }
    
    public CodeTableModel( SSMRunner sr, MachineState mst )
    {
    	ssmRunner = sr ;
    	machineState = mst ;
    	labels = new Labels( this ) ;
        reset() ;
    }
    
    protected void setPCNBWidths( JTable jt )
    {
        TableColumn tc ;
        int w = Images.white.getIconWidth() ;
        tc = jt.getColumn( getColumnName( C_PC ) ) ;
        tc.setPreferredWidth( 3*w/2 ) ;
        tc.setMaxWidth( 2*w ) ;
        tc = jt.getColumn( getColumnName( C_BP ) ) ;
        tc.setPreferredWidth( 3*w/2 ) ;
        tc.setMaxWidth( 2*w ) ;
    }
    
    protected void setScrollBarModel( BoundedRangeModel m )
    {
    	verticalScrollBarModel = m ;
    }

    public int getColumnCount()
    {
        return columnNames.length ;
    }
    
    public int getRowCount()
    {
        return rows.size() ;
    }

    public Vector<MetaInstruction> getMetaInstructionsAtPC()
    {
        int row = rowOfMemLoc( registers.getPC() ) ;
        return isValidRow( row ) ? getRowAt( row ).metaInstructions : null ;
    }

    public boolean isCellEditable( int row, int column )
    {
    	boolean res ;
	    Row r = rows.elementAt( row ) ;
	    res =  ( column == C_VALUE )
	    	|| ( column == C_INSTR )
	    	|| ( column == C_ARG1 && r.getNrInlineOpnds() > 0 )
	    	|| ( column == C_ARG2 && r.getNrInlineOpnds() > 1 )
	    	;
        return res ;
    }
    
    public Object getValueAt( int row, int column )
    {
    	Object res ;
	    Row r = rows.elementAt( row ) ;
    	if ( column == C_PC )
    	{
    		res = (r.memLoc == registers.getPC()) ? Images.redball : Images.white ;
    	}
    	else if ( column == C_BP )
    	{
    		res = r.hasBreakPoint ? Images.check : Images.white ;
    	}
    	else
    	{
    	    if ( column == C_VALUE )
    	    {
    	        res = memory.getAsHexAt( r.memLoc ) ;
    	    }
    	    else if ( column == C_LABEL && r.labelName != null )
    	    {
    	        res = r.labelName ;
    	    }
    	    else if ( column == C_ADDRESS )
    	    {
    	        res = Utils.asHex( r.memLoc ) ;
    	    }
    	    else if ( column == C_INSTR )
    	    {
    	        res = r.getInstrRepr( r.memLoc ) ;
    	    }
    	    else if ( column == C_ARG1 && r.getNrInlineOpnds() > 0 )
    	    {
    	        res = memory.getAsHexAt( r.memLoc+1 ) ;
    	    }
    	    else if ( column == C_ARG2 && r.getNrInlineOpnds() > 1 )
    	    {
    	        res = memory.getAsHexAt( r.memLoc+2 ) ;
    	    }
    	    else
    	        res = "" ;
    	}
        return res ;
    }

	
    public void setValueAt( Object aValue, int row, int column )
    {
    	boolean onlyThisRowChanged = true ;
	    Row r = rows.elementAt( row ) ;
	    String strValue ;
	    
        if ( aValue instanceof String )
            strValue = (String)aValue ;
        else
            strValue = aValue.toString() ;

	    if ( column == C_VALUE )
	    {
	    	Instruction instr = Instruction.findByCode( Utils.fromHex( strValue ) ) ;
	    	if ( instr != null )
	    	{
	    		onlyThisRowChanged = ! r.replaceInstr( row, instr ) ;
	    	}
	    }
	    else if ( column == C_ADDRESS )
	    {
	    }
	    else if ( column == C_INSTR )
	    {
	    	Instruction instr = Instruction.findByRepr( strValue ) ;
	    	if ( instr != null )
	    	{
	    		onlyThisRowChanged = ! r.replaceInstr( row, instr ) ;
	    	}
	    }
	    else if ( column == C_ARG1 )
	    {
	    	memory.setAt( r.memLoc + 1, strValue ) ;
	    }
	    else if ( column == C_ARG2 )
	    {
	    	memory.setAt( r.memLoc + 2, strValue ) ;
	    }

	    if ( ! onlyThisRowChanged )
	        fireTableRowsUpdated( row+1, getRowCount()-1 ) ;

    }
    
    public void setInstrArgAt( int row, int argOffset, int val )
    {
        Row r = rows.elementAt( row ) ;
        memory.setAt( r.memLoc + argOffset, val ) ;
    }

    public String getColumnName( int column )
    {
        return columnNames[ column ] ;
    }

    public Class getColumnClass( int column )
    {
    	if ( column == C_PC || column == C_BP )
    		return ImageIcon.class ;
    	else
	        return SSMRunner.tableModelColumnClass ;
    }

    public void cellChanged( MemoryCellEvent e )
    {
    	Object src = e.getSource() ;
    	//System.out.println( "memcell evt=" + e + " row=" + row ) ;
    	if ( src == memory && e.event == MemoryCellEvent.CELL )
    	{
        	int row = rowOfMemLoc( e.cellIndex ) ;
	    	if ( row >= 0 )
	    	{
	    		//fireTableChanged( new TableModelEvent( this, row ) ) ;
	    		fireTableRowsUpdated( row, row ) ;
	    		if ( ssmRunner != null &&  ! ssmRunner.isSettingUp() )
		    		ssmRunner.println( "Warning: code modified at " + Utils.asHex( e.cellIndex ) ) ;
    		}
    	}
    	else if ( src == registers && e.event == MemoryCellEvent.CELL )
    	{
    		if ( e.cellIndex == Registers.PC )
    		{
            	int row = rowOfMemLoc( registers.getReg( Registers.PC ) ) ;
            	if ( isValidRow( lastPCRow ) )
        			//fireTableChanged( new TableModelEvent( this, lastPCRow, lastPCRow, C_PC ) ) ;
        			fireTableCellUpdated( lastPCRow, C_PC ) ;
    			lastPCRow = row ;
            	if ( isValidRow( row ) )
            	{
        			fireTableCellUpdated( row, C_PC ) ;
        			//System.out.println( "scroll row " + row + " bar=" + verticalScrollBarModel ) ;
        			try
        			{
	        			int rowRange = getRowCount() ;
	        			int barRange = verticalScrollBarModel.getMaximum() - verticalScrollBarModel.getMinimum() ;
	        			int barToRowRatio = barRange / rowRange ;
	        			int barValue = verticalScrollBarModel.getValue() ;
	        			int barExtent = verticalScrollBarModel.getExtent() ;
	        			if ( row < ( barValue / barToRowRatio ) || row >= ( (barValue+barExtent) / barToRowRatio ) )
	        			{
	        			    int prefBarValue = row * barToRowRatio - barExtent / 2 ;
	        			    int newBarValue = Math.max( 0, Math.min( prefBarValue, barRange - barExtent ) ) ;
	        			    verticalScrollBarModel.setValue( newBarValue ) ;
	        			}
        			}
        			catch ( Exception ex )
        			{
        			}
    			}
    		}
    	}
    }
    
	public void handleBreakpointMouseEvent( int row, int column )
	{
	    if ( column == C_BP )
	    {
    	    Row r = getRowAt( row ) ;
    	    r.hasBreakPoint = ! r.hasBreakPoint ;
            //fireTableChanged( new TableModelEvent( this, row, row, column ) ) ;
            fireTableCellUpdated( row, column ) ;
        }
	}
	    
    protected void parseInitialize()
    {
    }
    
    protected String parseFinalize()
    {
        Vector<Labels.UnresolvedLabelUsage> v = labels.resolveUnresolved() ;
        return v.size() == 0 ? null : ("unresolved labels " + v) ;
    }
    
    protected String enterParsedLine( Vector<String> definedLabels, Vector<String> instrNArgs, Vector<String> leftOverLabels )
    {
        Instruction instr = null ;
        
        instr = Instruction.findByRepr( instrNArgs.elementAt(0).toLowerCase() ) ;
        
        if ( instr == null )
        {
            return "unknown instruction: " + instrNArgs ;
        }
        else
        {
            if ( (instrNArgs.size() - 1) < instr.getNrInlineOpnds() )
                return "not enough arguments for " + instr.getRepr() ;
            
            int insertionPos = getRowCount() - 1 ; // assuming halt at end
            if ( instr.isMeta() )
            {
                if ( insertionPos <= 0 )
                    return "cannot add meta at start of code (currently)" ;
                Row r = getRowAt( insertionPos-1 ) ;
                Utils.addAllTo( leftOverLabels, definedLabels.elements() ) ;
                instrNArgs.removeElementAt( 0 ) ;
                MetaInstruction m = instr.instantiateMeta( instrNArgs ) ;
                //System.out.println( "added meta " + m ) ;
                r.addMetaInstruction( m ) ;
            }
            else
            {
                insertNewInstrAt( insertionPos, true ) ;
                Row r = getRowAt( insertionPos ) ;
                for ( int i = 0 ; i < definedLabels.size() ; i++ )
    	        {
    	        	r.labelName = definedLabels.elementAt(i) ;
    	            labels.defineLabel( r.labelName, insertionPos ) ;
    	        }
                
                //System.out.println( "code table parse instr " + instr + " vec=" + toks ) ;
				setValueAt( instr.getRepr(), insertionPos, C_INSTR ) ;
                //int args[] = new int[ instr.getNrInlineOpnds() ] ;
                for ( int arg = 0 ; arg < instr.getNrInlineOpnds() ; arg++ )
                {
                    String s = instrNArgs.elementAt( arg + 1 ) ;
                    int val ;
                    if ( Utils.isNumberRepr( s, false ) )
                    {
                        val = Utils.fromHex( s, false ) ;
                    }
                    else
                    {
                        val = labels.useLabel( s, insertionPos, arg+1, instr.getNrMemCells(), instr.isRelativeOpnd(arg) ) ;
                        r.setUsedLabel( s ) ;
                    }
                    memory.setAt( r.memLoc + arg + 1, val ) ;
                    //args[ arg ] = val ;
                }
				//setValueAt( instr.getRepr( args ), insertionPos, C_INSTR ) ;
           }
        }
        
        //System.out.println( "added row=" + insertionPos + " loc=" + r.memLoc + " instr" + instr ) ;
        
        return null ;
    }

}
