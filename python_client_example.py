#!/usr/bin/env python3
"""
RuneLite Game State Reader Example

This script reads game state data exported by the RuneLite plugin
from the JSON file and demonstrates how to use it in Python.
"""

import json
import os
import time
from datetime import datetime
from pathlib import Path

class RuneLiteGameStateReader:
    def __init__(self):
        self.game_state_file = Path.home() / "runelite_game_state.json"
        self.last_position = None
        self.session_distance = 0
        
    def read_game_state(self):
        """Read the current game state from the JSON file"""
        try:
            if not self.game_state_file.exists():
                return None
                
            with open(self.game_state_file, 'r') as f:
                data = json.load(f)
                
            # Convert timestamp to readable format
            data['readable_time'] = datetime.fromtimestamp(data['timestamp'] / 1000).strftime('%H:%M:%S')
            
            return data
            
        except (json.JSONDecodeError, FileNotFoundError, KeyError) as e:
            print(f"Error reading game state: {e}")
            return None
    
    def calculate_session_distance(self, current_state):
        """Calculate distance moved this session"""
        if not current_state:
            return 0

        current_pos = (current_state['position']['x'], current_state['position']['y'])
        
        if self.last_position:
            # Simple 2D distance calculation
            dx = current_pos[0] - self.last_position[0]
            dy = current_pos[1] - self.last_position[1]
            distance = (dx**2 + dy**2)**0.5
            self.session_distance += distance
            
        self.last_position = current_pos
        return self.session_distance
    
    def print_game_info(self, state):
        """Print formatted game information"""
        if not state:
            print("No game state available")
            return
            
        print(state)
        print("\n" + "="*50)
        print(f"🎮 RuneLite Game State - {state['readable_time']}")
        print("="*50)
        print(f"👤 Player: {state['player_name']}")
        print(f"🎯 Game State: {state['game_state']}")
        print(f"❤️  HP: {state['hitpoints']['current']}/{state['hitpoints']['max']}")
        print(f"🏃 Total Distance: {state['total_distance_traveled']} tiles")
        print(f"In combat: {state['in_combat']}")
        print(f"Combat XP: {state['combat_xp']}")
        print(f"Deaths {state['deaths']}")

        # Health warning
        hp_percentage = (state['hitpoints']['current'] / state['hitpoints']['max']) * 100
        if hp_percentage < 30:
            print("⚠️  WARNING: Low health!")
        elif hp_percentage < 50:
            print("⚡ Health getting low")
            
    def monitor_continuous(self, update_interval=2):
        """Continuously monitor game state"""
        print("🚀 Starting RuneLite Game State Monitor...")
        print("📁 Reading from:", self.game_state_file)
        print("Press Ctrl+C to stop")
        
        try:
            while True:
                state = self.read_game_state()
                if state:
                    self.print_game_info(state)
                else:
                    print(f"⏳ Waiting for game state... (File: {self.game_state_file})")
                    
                time.sleep(update_interval)
                
        except KeyboardInterrupt:
            print("\n👋 Monitoring stopped by user")

def main():
    """Example usage of the RuneLite Game State Reader"""
    reader = RuneLiteGameStateReader()
    
    print("RuneLite Game State Reader")
    print("Choose an option:")
    print("1. Read current state once")
    print("2. Monitor continuously")
    print("3. Exit")
    
    choice = input("\nEnter choice (1-3): ").strip()
    
    if choice == "1":
        state = reader.read_game_state()
        reader.print_game_info(state)
        
    elif choice == "2":
        reader.monitor_continuous()
        
    elif choice == "3":
        print("👋 Goodbye!")
        
    else:
        print("❌ Invalid choice")

# Example functions for specific use cases
def check_if_player_moved(reader):
    """Example: Check if player has moved since last check"""
    current_state = reader.read_game_state()
    if current_state and reader.last_position:
        current_pos = (current_state['position']['x'], current_state['position']['y'])
        return current_pos != reader.last_position
    return False

def get_player_health_percentage(reader):
    """Example: Get player health as percentage"""
    state = reader.read_game_state()
    if state and state['hitpoints']['max'] > 0:
        return (state['hitpoints']['current'] / state['hitpoints']['max']) * 100
    return 0

def is_player_in_danger(reader, danger_zones=None):
    """Example: Check if player is in a dangerous area"""
    if danger_zones is None:
        danger_zones = [
            {"name": "Wilderness", "x_min": 2944, "x_max": 3392, "y_min": 3520, "y_max": 4000}
        ]
    
    state = reader.read_game_state()
    if not state:
        return False
    
    pos = state['position']
    for zone in danger_zones:
        if (zone['x_min'] <= pos['x'] <= zone['x_max'] and 
            zone['y_min'] <= pos['y'] <= zone['y_max']):
            return True, zone['name']
    
    return False, None

if __name__ == "__main__":
    main()

