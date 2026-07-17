import { Component, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './services/theme/theme.service';
import { FcmService } from './services/fcm/fcm.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly title = signal('NormaFlow');

  constructor(
    private themeService: ThemeService,
    private fcmService: FcmService
  ) { }

  ngOnInit(): void {
    this.themeService.init();

    // Iniciar Firebase Messaging
    this.fcmService.requestPermission();
    this.fcmService.listenForMessages();
  }
}
