//
//  CodeShelfPrefPane.m
//  Preference panel for CodeShelf
//
//  Copyright (C) 2010 Gadgetworks, LLC. All rights reserved.
//

#import <Security/Security.h>
#import <CoreFoundation/CoreFoundation.h>
#import "CodeShelfPrefPane.h"

@implementation codeshelfPrefPane

- (void)mainViewDidLoad
{
	AuthorizationItem authItems[1]; 	// we only want to get authorization for one command

    authItems[0].name = kAuthorizationRightExecute;	// we want the right to execute
	char *cmd = [[[NSBundle bundleForClass:[self class]] pathForAuxiliaryExecutable:@"HelperTool"] fileSystemRepresentation];
    authItems[0].value = cmd;		// the path to the helper tool
    authItems[0].valueLength = strlen(cmd);	// length of the command
    authItems[0].flags = 0;				// no extra flags
    
	AuthorizationRights authRights;
    authRights.count = 1;		// we have one item
    authRights.items = authItems;	// here is the values for our item
	[authView setAuthorizationRights:&authRights];
	[authView setAutoupdate:YES];
	[authView setDelegate:self];
	[authView updateStatus:self];

    [statusProgress setStyle:NSProgressIndicatorSpinningStyle];
    [statusProgress setDisplayedWhenStopped:NO];

	[self updateStatus];

	[NSTimer scheduledTimerWithTimeInterval: 2.0
									 target:self
								   selector:@selector(updateStatus) 
								   userInfo:nil 
									repeats:YES];
	
}

- (BOOL)isRunning
{
    FILE *ps;
    char buff[1024];
    
    if((ps=popen(psCmd, "r")) == NULL)
    {
        // There was an error opening the pipe. Alert the user.
        NSBeginAlertSheet(
            @"Error!",
            @"OK",
            nil,
            nil,
            [NSApp mainWindow],
            self,
            nil,
            nil,
            self,
            @"An error occured while detecting a running CodeShelf process.",
            nil);
        
        return NO;
    }
    else
    {
		BOOL running = NO;
        if(fgets(buff, 1024, ps)) {
			running = YES;
			printf("%s", buff);
		}
        pclose(ps);
        return running;
    }
}

- (BOOL)isLoaded
{
    FILE *ps;
    char buff[1024];
    
    if((ps=popen(launchCtlCmd, "r")) == NULL)
    {
        // There was an error opening the pipe. Alert the user.
        NSBeginAlertSheet(
						  @"Error!",
						  @"OK",
						  nil,
						  nil,
						  [NSApp mainWindow],
						  self,
						  nil,
						  nil,
						  self,
						  @"An error occured while detecting a running CodeShelf process.",
						  nil);
        
        return NO;
    }
    else
    {
		BOOL running = NO;
        if(fgets(buff, 1024, ps)) {
			running = YES;
			printf("%s", buff);
		}
        pclose(ps);
        return running;
    }
	
}

- (IBAction)toggleAutoStart:(id)sender
{	
	char *args[2];
	args[0] = "boot"; 
	args[1] = NULL;
	
	OSStatus ourStatus = AuthorizationExecuteWithPrivileges([[authView authorization] authorizationRef],
															[authView authorizationRights]->items[0].value,
															kAuthorizationFlagDefaults, args, NULL);
	
	if(ourStatus != errAuthorizationSuccess)
	{
		// alert user the startup has failed
		NSBeginAlertSheet(
						  @"Error!",
						  @"OK",
						  nil,
						  nil,
						  [NSApp mainWindow],
						  self,
						  nil,
						  nil,
						  self,
						  @"Could not toggle CodeShelf startup.",
						  nil);
		[statusTimer invalidate];
		[self checkStatus];
	}

//	NSMutableDictionary* plistDict = [[NSMutableDictionary alloc] initWithContentsOfFile:launchAgentPath];	
//	NSNumber *keyValue = [plistDict objectForKey:@"RunAtLoad"];
//
//	NSLog(@"Desc = %@", [keyValue className]);
//	NSLog(@"Value = %@", keyValue);
//	
//	if ([keyValue boolValue] == YES)
//		keyValue = [NSNumber numberWithBool:NO];
//	else
//		keyValue = [NSNumber numberWithBool:YES];
//		
//	NSLog(@"Desc = %@", [keyValue className]);
//	NSLog(@"Value = %@", keyValue);
//
//	[plistDict setObject:keyValue forKey:@"RunAtLoad"];
//	BOOL success = [plistDict writeToFile:launchAgentPath atomically: NO];
//	
//	NSLog(@"Success = %@", success);

	[self updateStatus];
}

- (IBAction)toggleServer:(id)sender
{
	[statusMessage setHidden:YES];
	[statusProgress startAnimation:self];
    [startButton setEnabled:NO];
	
    if(![self isRunning])
    {
        if (![self isLoaded])
		{
			[self loadServer];
			statusTimer = [NSTimer scheduledTimerWithTimeInterval:4 target:self selector:@selector(checkStatus) userInfo:nil repeats:NO];
		}
		else
		{		
			[self startServer];
			statusTimer = [NSTimer scheduledTimerWithTimeInterval:4 target:self selector:@selector(checkStatus) userInfo:nil repeats:NO];
		}
    }
    else
    {
        [self stopServer];
        statusTimer = [NSTimer scheduledTimerWithTimeInterval:4 target:self 
            selector:@selector(checkStatus) userInfo:nil repeats:NO];
    }
    [self updateStatus];
}

- (void)checkStatus
{
	[statusProgress stopAnimation:self];
	[statusMessage setHidden:NO];
    [startButton setEnabled:YES];
    [self updateStatus];
}


- (void)updateStatus
{
	if ([self isRunning] == NO)
	{
		[statusMessage setStringValue:@"Stopped"];
		[statusMessage setTextColor:[NSColor redColor]];
		[statusDescription setStringValue:@"The server may take a few seconds to start up."];
		[startButton setTitle:@"Start CodeShelf"];
	}
	else 
	{
		[statusMessage setStringValue:@"Running"];
		[statusMessage setTextColor:[NSColor greenColor]];
		[statusDescription setStringValue:@"The server may take a few seconds to stop."];
		[startButton setTitle:@"Stop CodeShelf"];
	}
	BOOL isStartingAtBoot = [[[NSMutableDictionary dictionaryWithContentsOfFile:launchAgentPath] objectForKey:@"RunAtLoad"] boolValue];
	[autoStartCheckbox setState:(isStartingAtBoot ? NSOnState : NSOffState)];
}

- (void)loadServer
{

	[[NSTask launchedTaskWithLaunchPath:launchctl arguments:[NSArray arrayWithObjects:@"load", launchAgentPath,  nil]] waitUntilExit];
}

- (void)startServer
{
	[[NSTask launchedTaskWithLaunchPath:launchctl arguments:[NSArray arrayWithObjects:@"start", launchAgent,  nil]] waitUntilExit];
}

- (void) stopServer
{
	[[NSTask launchedTaskWithLaunchPath:launchctl arguments:[NSArray arrayWithObjects:@"stop", launchAgent,  nil]] waitUntilExit];
}

@end
