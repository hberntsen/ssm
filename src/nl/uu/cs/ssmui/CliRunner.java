package nl.uu.cs.ssmui;

import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Vector;

import nl.uu.cs.ssm.Machine;
import nl.uu.cs.ssm.MachineState;
import nl.uu.cs.ssm.Messenger;
import nl.uu.cs.ssm.MetaInstruction;

public class CliRunner implements Messenger {
	private static final long STEPS_INFINITE = -1;
	
	private long steps;
	private MachineState machineState = new MachineState(5000, 2000, this);
	private Machine machine = new Machine(machineState, this);
	private StepManager stepManager = new StepManager(machine);
	private CodeTableModel codeTableModel= new CodeTableModel(null, machineState);
	   
	
	public CliRunner(File f,long steps) {
		this.steps = steps;
		loadFile(f);
	}
	
	public void run() {
		   long count = 0;
		   while (count != steps) {
			   if(doAStepForward()) {
				   return;
			   }
			   
			   if(count != STEPS_INFINITE) {
				   count ++;
			   }
		   }
	}
	
	/**
	 * 
	 * @return True if we are halted
	 */
	protected boolean doAStepForward()
	{
        stepManager.beginForwardStep() ;
        //Vector<MetaInstruction> metaInstructions = codeTableModel.getMetaInstructionsAtPC() ;
        machine.executeOne() ;
        if ( machineState.isHalted() )
            return true;
        /*
        if ( metaInstructions != null )
        {
            for ( Enumeration<MetaInstruction> e = metaInstructions.elements() ; e.hasMoreElements() ; )
            {
                MetaInstruction mi = e.nextElement() ;
                mi.exec( machineState ) ;
            }
        }
        */
        stepManager.endForwardStep() ;
        return false;
	}
	
	private void reset()
	{
	    //stopContinuouslyDoingSteps() ;

		codeTableModel.beforeReset() ;
		
		machine.reset() ;
		machineState = machine.getMachineState() ;
		
		codeTableModel.reset() ;
		//stackTableModel.reset() ;
		//statusTableModel.reset() ;
		//heapTableModel.reset();
	}
	
	private void resetToInitialState()
	{
		machineState.resetToInitialState() ;
		//stackTableModel.reset() ;
		//heapTableModel.reset();
	}	
	
	protected void loadFile( File f )
	{
		String msg ;
	    try
	    {
	    	Vector<String> leftOverLabels ;
	    	AssemblyParseResult apr ;
	        FileReader fr = new FileReader( f ) ;
	        AssemblyParser ap = new AssemblyParser( fr ) ;
			reset() ;
			codeTableModel.parseInitialize() ;
	        for ( apr = null ; ! ap.isAtEOF() ; )
	        {
	            apr = ap.parse1Line( apr ) ;
	            if ( apr.message != null )
	                println( "Line " + apr.lineNr + ": " + apr.message ) ;
	            else if ( apr.instrNArgs.size() > 0 )
	            {
	                leftOverLabels = new Vector<String>() ;
	                msg = codeTableModel.enterParsedLine( apr.definedLabels, apr.instrNArgs, leftOverLabels ) ;
    			    if ( msg != null )
        			    println( "Line " + apr.lineNr + ": " + msg ) ;
	                if ( leftOverLabels.size() == 0 )
	                    apr = null ;
	                else
	                    apr.addLabels( leftOverLabels ) ;
	            }
	        }
			fr.close() ;
		}
		catch ( Exception ex )
		{
		    ex.printStackTrace() ;
		}
		finally
		{
		    msg = codeTableModel.parseFinalize() ;
		    if ( msg != null )
		        println( msg ) ;
		}
		resetToInitialState() ;
	}

	@Override
	public void println(String s) {
		System.out.println(s);
	}
}
