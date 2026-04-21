import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/layout/header/header.component';
import { HttpClient } from '@angular/common/http';
import { UserProfile } from './models/user.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HeaderComponent, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<UserProfile>('http://localhost:8081/api/users/me').subscribe({
      next: (ctx) => console.log('[/me] UserContext reçu :', ctx),
      error: (err) => console.error('[/me] Erreur :', err)
    });
  }
}