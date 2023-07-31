import java.util.ArrayList;

public class Data {
	String rawData =
		"""
		Guide
		120, 10, 5, 5
		Boomerang
		true
		You're not sure why you're here?
		Good. None of us really are.
		It doesn't matter all that much anyways.
		...
		\\
		Princess
		20
		Loop
		false
		I've always wanted a knight in shining armor to come and save me.
		To fulfill my wish, I had once placed myself into danger.
		My knight never came.
		I guess I'm a dare-devil, hehe...
		\\
		Question Mark
		60, 10, 60
		Boomerang
		false
		Who are you?
		Won't you give me an answer..?
		\\
		Not a Cat
		1
		Static
		true
		Meow! I'm a cat! Meow!
		Don't believe me? Meow!
		Meow! Meow!
		Just trust in me! Meow!
		Meow, meow!
		I just wish that you would believe in me...
		\\
		Spectator
		120, 5, 5, 5, 5, 5, 120
		Boomerang
		true
		No matter what you do, you won't be able to change this world.
		After all, we're denizens whose times have stopped.
		Unable to move forward, we can only watch.
		I may be stuck here as a spectator, but I can find some enjoyment in observing others.
		Go on now. You don't have to worry about me.
		I'm the one who put myself here.
		\\
		Lazy
		1
		Static
		false
		I will not move from here...
		I don't see a reason to.
		It would simply be wasteful.
		I don't understand people like you.
		\\
		Searcher
		60, 5, 5, 5, 60 
		Boomerang
		true
		All this time, I've been seeking a precious thing of mine.
		The problem is, I can't quite remember what it was. 
		\\
		Unknown
		1
		Loop
		false
		Who am I? Why do you care?
		What's my purpose? How would I know?
		Purpose is just an inane construct.
		And it's no use if it's all in your head.
		If you really think about it, "purpose" is quite the purposeless word.
		Your purpose? Well, I don't have the answer you're looking for.
		\\
		Quiet
		300, 1
		Boomerang
		true
		I'm sorry. I'm not really up for conversation right now.
		...
		\\
		Morning
		20
		Loop
		false
		Good morning. How are you today?
		I'm doing quite fine myself.
		I'm glad you asked.
		Vitamin D sure does wonders for your bones.
		An apple a day keeps the doctor away.
		Great weather, isn't it?
		Haha, I knew you'd say that.
		Stay away from sugar, stay away from grease.
		Fish oil will make your liver strong.
		...
		\\
		Wavering
		60, 5, 5, 5, 5, 5, 60
		Boomerang
		false
		Sometimes, when I'm with people, I just want to be left alone.
		But when I'm alone, I feel so lonely.
		I don't know what I want.
		But that's okay.
		I can't afford to be that self-centered.
		\\
		Toaster
		1
		Static
		false
		Everyone says it's my fault.
		But I know...that I didn't do it.
		They're just too sensitive.
		They're just misunderstanding the situation.
		Everyone says it's my fault...
		\\
		Sunny Side Up
		1
		Static
		false
		I am nourishment for you.
		\\
		Narcissist
		120, 5
		Loop
		false
		Hehehe...
		You don't deserve to look at me.
		\\
		Spinning Square
		3
		Loop
		false
		I'm in a loop, I'm in a loop...
		\\
		Excited
		3
		Loop
		true
		I'm so excited for tomorrow!
		Tomorrow is going to be the best day ever!
		Today has been such a slog...
		But tomorrow will be better!
		Because today's problems will be over!
		If only I could reach tomorrow...
		\\
		Walter
		1
		Static
		true
		Hey there, pal!
		How's it goin'?
		I hope you feel real comfy 'round here.
		Some of us lot don't really like strangers.
		But I just wanted to let you know...
		...that you're welcome here, any time.
		Just kidding.
		Get out, shit head.
		\\
		Snowman
		120, 5, 5, 5, 120
		Boomerang
		true
		I've been told that I should always look forward.
		Even if I'm faced with seemingly impossible hurdles...
		Everyone says, "Well, you'll get over it if you try hard enough".
		Or something like that.
		But it gets tiring.
		If I climb over one hurdle, I'll just be faced with another.
		And at some point, one of these hurdles will be truly impossible.
		I mean, look at me. I'm a snowman.
		I don't even have legs.
		How am I supposed to get over hurdles?		
		\\
		""";
		
	ArrayList<NPCData> parsedData = new ArrayList<NPCData>();
	
	public Data() {
		String[] splitData = rawData.split("\n");
		int step = 0;
		NPCData object = new NPCData();
		ArrayList<String> dialogue = new ArrayList<String>();
		
		// Parsing the string to a usable form
		for(String line : splitData) {
			switch(step) {
				case 0: // Name
					object = new NPCData();
					object.name = line;
					step++;
					break;
				case 1: // Frame count array
					String[] framesStr = line.split(", ");
					int[] frames = new int[framesStr.length];
					for(int i = 0; i < framesStr.length; i++) {
						frames[i] = Integer.parseInt(framesStr[i]);
					};
					object.frames = frames;
					step++;
					break;
				case 2: // Animation Method
					object.animation = line;
					step++;
					dialogue = new ArrayList<String>();
					break;
				case 3: // Flippability
					object.flippable = Boolean.parseBoolean(line);
					step++;
					break;
				case 4: // Dialogue
					if(!line.equals("\\")) {
						dialogue.add(line);
					} else {
						object.dialogue = dialogue.toArray(new String[0]);
						step = 0;
						parsedData.add(object);
					};
					break;
			};
		};
	};
	
	public String getRaw() {
		return rawData;
	};
	
	public NPCData getNPCData(int i) {
		if(i == -2) {
			NPCData exitData = new NPCData();
			exitData.name = "Exit";
			int[] arr = {120, 5, 5, 5, 120};
			exitData.frames = arr;
			exitData.animation = "Boomerang";
			String[] strArr = {
					"Do you truly wish to depart? You will leave nothing behind.",
					"The world will remain unchanged. Can you accept that?",
					"Then go. Your will is your most prized possession."
			};
			exitData.dialogue = strArr;
			exitData.flippable = false;
			return exitData;
		};
		return parsedData.get(i);
	};
}
