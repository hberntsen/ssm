/* 
	Runner.java

	Title:			Simple Stack Machine Runner
	Author:			atze
	Description:	
*/

package nl.uu.cs.ssmui;

import java.io.File;

import javax.swing.UIManager;

import nl.uu.cs.ssm.Config;

public class Runner extends Thread
{
    protected int delay = 50 ;
    
    SSMRunner  ssmRunner  ;
    
	public Runner( File initialFile ) 
	{
		try {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} 
			catch (Exception e) { 
			}
		    ssmRunner = new SSMRunner( this );
			ssmRunner.initComponents();
			ssmRunner.setVisible(true);
			//System.out.println( "Foc Trav=" + ssmRunner.isFocusTraversable() ) ;
			ssmRunner.requestFocus() ;
			if ( initialFile != null )
				ssmRunner.loadFile( initialFile ) ;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
   public static void usage() {
	   System.out.println("Simple Stack Machine Interpreter");
	   System.out.println("Version " + Config.version() + ", " + Config.versionDate());
	   System.out.println("usage: [--clisteps <steps>] <file>");
	   System.out.println("\t--clisteps <steps>: The amount of steps to run. -1 for infinite. When passed, enables cli mode");
	   System.exit(1);
   }
	
	
	public void run()
	{
		while( true )
		{
			int steppingState = ssmRunner.steppingState() ;
			if ( steppingState != SSMRunner.STEP_BY_STEP )
			{
				if ( ssmRunner.hasBreakpointAtPC() )
					ssmRunner.stopContinuouslyDoingSteps() ;
				else if ( steppingState == SSMRunner.STEP_CONT_FORWARD )
					ssmRunner.doAStepForward() ;
				else if ( steppingState == SSMRunner.STEP_CONT_BACKWARD )
					ssmRunner.doAStepBack() ;
			}
			try { sleep( delay ) ; } catch ( InterruptedException e ) {}
		}
	}

	// Main entry point
	static public void main(String[] args) {
		File initialFile = null;
		long steps = -1;

		if (args.length > 1) {
			for (int i = 0;; i += 2) {
				if (i > args.length - 1) {
					usage();
				}
				if (i == (args.length - 1)) {
					initialFile = new File(args[i]);
					break;
				}
				String key = args[i];
				String value = args[i + 1];
				if (key.equals("--clisteps")) {
					steps = Long.parseLong(value);
				} else {
					usage();
				}
			}
			new CliRunner(initialFile, steps).run();
		} else {

			if (args.length > 0) {
				File f = new File(args[0]);
				if (f.exists())
					initialFile = f;
			}
			new Runner(initialFile);
		}
	}
	
}
