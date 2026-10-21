import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/layout/header/header.component';
import { HttpClient } from '@angular/common/http';
import { UserProfile } from './models/user.model';
import { environment } from '../environments/environment';

@Component({
   selector: 'app-root',
   standalone: true,
   imports: [HeaderComponent, RouterOutlet],
   templateUrl: './app.component.html',
   styleUrls: ['./app.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent implements OnInit {
   private readonly http = inject(HttpClient);

   ngOnInit(): void {
      this.http
         .get<UserProfile>(`${environment.apiUrl}/users/me`)
         .subscribe({
            next: (ctx) => console.log('[/me] UserContext reçu :', ctx),
            error: (err) => console.error('[/me] Erreur :', err),
         });
   }
}
