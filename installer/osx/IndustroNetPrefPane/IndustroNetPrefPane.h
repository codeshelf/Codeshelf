//
//  CodeShelfPrefPane.h
//  Preference panel for CodeShelf
//
//  Copyright (C) 2010 Gadgetworks, LLC. All rights reserved.
//

#import <PreferencePanes/PreferencePanes.h>
#import <Security/Security.h>
#import <SecurityInterface/SFAuthorizationView.h>
#include <unistd.h>

// 'ps' command to use to check for running CodeShelf daemon
char *psCmd = "/bin/ps auxww | fgrep -v 'fgrep' | fgrep CodeShelf.app";

// 'launchctl' command to use to check for loaded CodeShelf daemon
char *launchCtlCmd = "/bin/launchctl list | fgrep -v 'fgrep' | fgrep com.gadgetworks.CodeShelf";

// The path to the plist file
NSString *const launchAgentPath = @"/Library/LaunchAgents/com.gadgetworks.CodeShelf.AutoRun.plist";
NSString *const launchAgent = @"com.gadgetworks.CodeShelf";
NSString *const launchctl = @"/bin/launchctl";

@interface codeshelfPrefPane : NSPreferencePane 
{
	IBOutlet NSButton *startButton;
	IBOutlet NSButton *autoStartCheckbox;
	IBOutlet NSButton *viewAdminButton;
	IBOutlet NSTextField *statusMessage;
	IBOutlet NSTextField *statusDescription;
	IBOutlet NSProgressIndicator *statusProgress;
	IBOutlet SFAuthorizationView *authView;
	
	NSTimer *statusTimer;
}

- (IBAction)toggleServer:(id)sender;
- (IBAction)toggleAutoStart:(id)sender;
- (void)mainViewDidLoad;
- (void)updateStatus;
- (void)loadServer;
- (void)startServer;
- (void)stopServer;
- (void)checkStatus;
- (BOOL)isRunning;
- (BOOL)isLoaded;

@end
